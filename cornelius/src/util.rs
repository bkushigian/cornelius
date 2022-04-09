use crate::subjects::AnalysisResult;
use std::collections::{HashMap, HashSet};
use std::hash::Hash;
use std::str::FromStr;

#[derive(Debug, PartialEq, Eq)]
/// Track an equality relation. This is used to test equivalence class files
/// output by a cornelius run, and to test against hand-crafted ground truth
/// files.
pub struct EqRel<T: PartialEq + Eq + Hash + std::fmt::Debug> {
    /// A vec of the equivalence classes
    classes: Vec<HashSet<T>>,
    /// A lookup map from elements to the index of containing equivalence class
    /// in `classes`
    elem_map: HashMap<T, usize>,
}

impl<T: Eq + Hash + std::fmt::Debug> EqRel<T> {
    /// Get a vec of equiv classes
    #[allow(dead_code)]
    pub fn classes(&self) -> &Vec<HashSet<T>> {
        &self.classes
    }

    /// Lookup an element
    ///
    /// # Examples
    ///
    /// ```
    /// use cornelius::util::EqRel;
    /// use std::collections::HashSet;
    /// use std::iter::FromIterator;
    ///
    /// let rel = EqRel::<u32>::from("0 1 2|3 4|5 6 7");
    /// assert_eq!(rel.lookup(&0), Some(&HashSet::from_iter(vec![0, 1, 2])));
    /// assert_eq!(rel.lookup(&1), Some(&HashSet::from_iter(vec![0, 1, 2])));
    /// assert_eq!(rel.lookup(&2), Some(&HashSet::from_iter(vec![0, 1, 2])));
    /// assert_eq!(rel.lookup(&3), Some(&HashSet::from_iter(vec![3, 4])));
    /// assert_eq!(rel.lookup(&4), Some(&HashSet::from_iter(vec![3, 4])));
    /// assert_eq!(rel.lookup(&5), Some(&HashSet::from_iter(vec![5, 6, 7])));
    /// assert_eq!(rel.lookup(&6), Some(&HashSet::from_iter(vec![5, 6, 7])));
    /// assert_eq!(rel.lookup(&7), Some(&HashSet::from_iter(vec![5, 6, 7])));
    /// ```
    pub fn lookup(&self, elem: &T) -> Option<&HashSet<T>> {
        self.classes.get(*self.elem_map.get(&elem)?)
    }

    /// Get the elements tracked by this `EqRel`
    ///
    /// # Examples
    ///
    pub fn elems(&self) -> HashSet<&T> {
        self.elem_map.keys().collect()
    }

    /// Check if this EqRel is a refinement of another EqRel. This means that:
    /// 1. `self.elems().is_subset(&other.elems())`
    /// 2. For each `elem` in `self.elems()`, `self.lookup(elem).is_subset(other.lookup(elem))`
    ///
    /// # Examples
    ///
    /// ```rust
    /// use cornelius::util::EqRel;
    /// assert!(EqRel::<u32>::from("0|1|2|3|4").is_refinement_of(&EqRel::<u32>::from("0 1 2 3 4")));
    /// assert!(EqRel::<u32>::from("0 1 2|3|4").is_refinement_of(&EqRel::<u32>::from("0 1 2|3 4")));
    /// ```
    pub fn is_refinement_of(&self, other: &Self) -> bool {
        self.elems().is_subset(&other.elems())
            && self.elems().iter().all(|x| {
                let my_class = self.lookup(*x).unwrap_or_else(|| {
                    panic!(
                        "Error: EqRel has element unassociated with a class: {:?}\nSelf: {:?}",
                        x, self
                    )
                });
                let their_class = other
                    .lookup(*x)
                    .expect("Error: EqRel has element unassociated with a class");
                my_class.is_subset(their_class)
            })
    }

    /// Restrict this EqRel to a subdomain
    /// # Examples
    ///
    /// ```rust
    /// # use cornelius::util::EqRel;
    /// # use std::collections::HashSet;
    /// # use std::iter::FromIterator;
    /// let domain: HashSet<u32> = HashSet::from_iter(vec![1, 3, 5, 7, 9]);
    /// let rel = EqRel::<u32>::from("0 1 2|3 4 5|6 7 8");
    /// let restricted = rel.restrict_to_domain(domain);
    /// let expected = EqRel::<u32>::from("1|3 5|7");
    /// assert_eq!(restricted, expected);
    /// ```
    pub fn restrict_to_domain(&self, domain: HashSet<T>) -> Self
    where
        T: Clone,
    {
        let mut classes = vec![];
        let mut elem_map: HashMap<T, usize> = HashMap::default();
        for class in self.classes.iter() {
            let class: HashSet<T> = class.intersection(&domain).cloned().collect();
            let idx = classes.len();
            for e in class.iter() {
                elem_map.insert(e.clone(), idx);
            }
            if !class.is_empty() {
                classes.push(class);
            }
        }
        EqRel { classes, elem_map }
    }

    /// Count the number of equivalences in this equivalence relation. This is
    /// calculated to be the number of elements minus the number of equivalence
    /// classes. Thus if an equivalnce class has size N, it counts as N-1
    /// equivalences. This is because at least N-1 equivalences must be
    /// witnessed for all N elements to be in the same equivalnce class.
    ///
    /// # Example
    ///
    /// ```
    /// use cornelius::util::EqRel;
    /// let eq_rel = EqRel::<u32>::from("0 1 2|3 4|5 6 7");
    /// assert_eq!(eq_rel.num_equivalences(), 5);
    /// let eq_rel = EqRel::<u32>::from("0|1|2|3|4|5");
    /// assert_eq!(eq_rel.num_equivalences(), 0);
    /// ```
    pub fn num_equivalences(&self) -> usize {
        self.elems().len() - self.classes.len()
    }
}

impl<'a, T: FromStr + Eq + Hash + Copy + std::fmt::Debug> From<&str> for EqRel<T>
where
    <T as std::str::FromStr>::Err: std::fmt::Debug,
{
    fn from(s: &str) -> Self {
        let mut classes = vec![];
        let mut elem_map = HashMap::default();
        for line in s.split(|c| c == '\n' || c == '|') {
            let mut class = HashSet::default();
            let idx = classes.len();
            for elem in line.split_whitespace() {
                let elem = elem
                    .parse()
                    .unwrap_or_else(|_| panic!("Element {} is not a valid T", elem));
                class.insert(elem);
                elem_map.insert(elem, idx);
            }
            classes.push(class);
        }
        EqRel { classes, elem_map }
    }
}

impl From<&AnalysisResult> for EqRel<u32> {
    fn from(ar: &AnalysisResult) -> EqRel<u32> {
        let classes: Vec<HashSet<u32>> = ar.equiv_classes.clone();
        let mut elem_map: HashMap<u32, usize> = HashMap::default();
        for (idx, class) in classes.iter().enumerate() {
            for &elem in class {
                elem_map.insert(elem, idx);
            }
        }
        EqRel { classes, elem_map }
    }
}

pub mod io {
    use crate::config::RunConfig;
    use crate::global_data::GlobalData;
    use crate::peg::{Peg, PegAnalysis};
    use crate::subjects::{Subject, Subjects};
    use egg::Runner;
    use indexmap::IndexMap;
    use itertools::Itertools;
    use std::fs::File;
    use std::io::prelude::*;
    use std::io::Error;

    /// Write the results to a single file
    #[allow(dead_code)]
    pub fn write_subjects_to_single_file(subjects: &Subjects, file: &str) -> Result<(), Error> {
        let mut file = File::create(file)?;

        // A list of subject content. Each entry should track a single file's contents
        let mut equiv_file_contents = vec![];
        for subject in &subjects.subjects {
            equiv_file_contents.push(get_equiv_file_contents_for_subject(subject));
        }

        let file_contents = equiv_file_contents.join("\n");
        file.write_all(file_contents.as_bytes())
    }

    /// Write results to individual files, one for each subject
    pub fn write_subjects_to_separate_files(subjects: &Subjects, dir: &str) -> Result<(), Error> {
        let mut i = 0;
        for subject in &subjects.subjects {
            i += 1;
            let mut file_name = String::from(dir);
            file_name.push('/');
            let idx = &subject.method.chars().position(|c| c == ':').unwrap();
            file_name.push_str(&subject.method[0..*idx]);
            file_name.push_str(&i.to_string());
            file_name.push_str(".equiv-class");
            file_name = file_name.chars().map(|c| match c {
                '<' => '[',
                '>' => ']',
                _   => c,
            }).collect();
            println!("{}", file_name);
            match write_subject_to_file(subject, &file_name) {
                Err(e) => {
                    println!(
                        "Error writing subject to file {}:{}", subject.source_file, subject.method
                    );
                    println!("    {}", e);
                }
                Ok(()) => (),
            }
            debug!("Successfully wrote subject {}", subject.method);
        }
        Ok(())
    }

    pub fn write_subject_to_file(subject: &Subject, file_name: &str) -> Result<(), Error> {

        let mut file = File::create(&file_name)?;
        info!("Writing subject to file {}", &file_name);
        let contents = get_equiv_file_contents_for_subject(subject);
        debug!("Contents:\n{}\n", &contents);
        file.write_all(contents.as_bytes())
    }

    fn get_equiv_file_contents_for_subject(subject: &Subject) -> String {
        let ar = &subject.analysis_result;
        let mut equiv_classes_as_strings = vec![];
        for equiv_class in &ar.equiv_classes {
            if equiv_class.len() == 1 && equiv_class.iter().next().unwrap() == &0 {
                continue;
            }
            let equiv_class_as_string: String = itertools::sorted(equiv_class)
                .iter()
                .map(|id| (**id).to_string())
                .intersperse(" ".to_string())
                .collect();
            equiv_classes_as_strings.push(equiv_class_as_string);
        }
        format!("{}\n", equiv_classes_as_strings.join("\n"))
    }

    /// Write details of the run to disk
    pub fn write_run_details_to_file(
        run_config: &RunConfig,
        global_data: &GlobalData,
        file_name: &str,
    ) -> Result<(), Error> {
        let mut file = File::create(file_name)?;
        info!(
            "Writing configuration and global data to file {}",
            file_name
        );
        let contents = format!("{}\n{}", run_config.to_string(), global_data.to_string());
        file.write_all(contents.as_bytes())
    }

    pub fn write_iter_file(
        file_name: String,
        subject_file: String,
        method_names: Vec<String>,
        runner: &Runner<Peg, PegAnalysis>,
    ) -> Result<(), Error> {
        let search_time: f64 = runner.iterations.iter().map(|i| i.search_time).sum();
        let apply_time: f64 = runner.iterations.iter().map(|i| i.apply_time).sum();
        let rebuild_time: f64 = runner.iterations.iter().map(|i| i.rebuild_time).sum();
        let total_time: f64 = runner.iterations.iter().map(|i| i.total_time).sum();
        let iters = runner.iterations.len();
        let rebuilds: usize = runner.iterations.iter().map(|i| i.n_rebuilds).sum();
        let stop_reason = runner.stop_reason.as_ref().unwrap();
        let methods = method_names.join("\n- ");
        let eg = &runner.egraph;

        let mut applied: IndexMap<String, usize> = IndexMap::new();
        for i in &runner.iterations {
            for (k, v) in i.applied.iter() {
                applied.insert(k.clone(), v + applied.get(k).unwrap_or(&0));
            }
        }

        let mut applications = String::from("");
        for (k, v) in applied.iter() {
            applications.push_str(format!("  - {}: {}\n", k, v).as_str())
        }

        info!("Writing iter file {}", file_name);
        let mut file = File::create(file_name.clone())?;
        let contents = format!(
            "Run Report for Subject {}
- {}
#nodes: {}
#eclasses: {}
#egraph-total-size: {}
search-time: {}
apply-time: {}
rebuild-time: {}
total-time: {}
#iters: {}
#rebuilds: {}
stop-reason: {:?}
applications:
{}",
            subject_file,
            methods,
            eg.total_number_of_nodes(),
            eg.number_of_classes(),
            eg.total_size(),
            search_time,
            apply_time,
            rebuild_time,
            total_time,
            iters,
            rebuilds,
            stop_reason,
            applications
        );
        file.write_all(contents.as_bytes())
    }
}

pub mod helpers {
    #[macro_export]
    macro_rules! map(
        { $($key:expr => $value:expr),+ } => {
            {
                let mut m = ::std::collections::HashMap::new();
                $(
                    m.insert($key, $value);
                )+
                m
            }
        };
    );
}

#[cfg(test)]
mod eq_rel {
    use super::EqRel;
    #[test]
    fn from_str() {
        let rel: EqRel<u32> = EqRel::from("1 2 | 3 | 4 | 5");
        assert!(rel.classes().len() == 4);
        let class1 = rel.lookup(&1).unwrap();
        let class2 = rel.lookup(&2).unwrap();
        let class3 = rel.lookup(&3).unwrap();
        let class4 = rel.lookup(&4).unwrap();
        let class5 = rel.lookup(&5).unwrap();
        assert!(class1 == class2);
        assert!(class1.len() == 2);
        assert!(class3.len() == 1);
        assert!(class4.len() == 1);
        assert!(class5.len() == 1);

        let eq_rel1: EqRel<u32> = EqRel::from("0 1 2 3 4 | 5 6 7 8 9| 10 11 | 12 13");
        let eq_rel2: EqRel<u32> = EqRel::from(
            "0 1 2 3 4
5 6 7 8 9
10 11
12 13",
        );
        assert!(eq_rel1 == eq_rel2);
    }

    #[test]
    fn refines() {
        let course: EqRel<u32> = EqRel::from("1 2 3 | 4 5 | 6 7 8 | 9 10");
        let fine: EqRel<u32> = EqRel::from("1 2 3 | 4 | 5 | 6 | 7 8 | 9 | 10");
        assert!(fine.is_refinement_of(&course));
    }

    #[test]
    fn restrict_to_domain() {
        let eq_rel1: EqRel<u32> = EqRel::from("0 1 | 2 3 4 | 5 ");
        let eq_rel2: EqRel<u32> = EqRel::from("0 1 2 3 4");

        let restricted = eq_rel1.restrict_to_domain(eq_rel2.elems().iter().map(|x| **x).collect());

        assert!(restricted.elems().contains(&0));
        assert!(restricted.elems().contains(&1));
        assert!(restricted.elems().contains(&2));
        assert!(restricted.elems().contains(&3));
        assert!(restricted.elems().contains(&4));
        assert!(!restricted.elems().contains(&5));
        assert!(restricted.is_refinement_of(&eq_rel1));
        assert!(restricted.lookup(&0) == restricted.lookup(&1) && restricted.lookup(&0).is_some());
        assert!(restricted.lookup(&2) == restricted.lookup(&3) && restricted.lookup(&2).is_some());
        assert!(restricted.lookup(&2) == restricted.lookup(&4));
        assert!(restricted.lookup(&1) != restricted.lookup(&4));

        let eq_rel1: EqRel<u32> = EqRel::from("0 1 2 3 4|5 6 7 8 9| 10 11| 12 13");
        let eq_rel2: EqRel<u32> = EqRel::from("1 3 5 7 9 11 13");
        let restricted = eq_rel1.restrict_to_domain(eq_rel2.elems().iter().map(|x| **x).collect());
        assert!(restricted.elems().contains(&1));
        assert!(restricted.elems().contains(&3));
        assert!(restricted.elems().contains(&5));
        assert!(restricted.elems().contains(&7));
        assert!(restricted.elems().contains(&9));
        assert!(restricted.elems().contains(&11));
        assert!(!restricted.elems().contains(&0));
        assert!(!restricted.elems().contains(&2));
        assert!(!restricted.elems().contains(&4));
        assert!(!restricted.elems().contains(&6));
        assert!(!restricted.elems().contains(&8));
        assert!(!restricted.elems().contains(&10));

        assert!(
            restricted.lookup(&0).is_none()
                && restricted.lookup(&2).is_none()
                && restricted.lookup(&4).is_none()
                && restricted.lookup(&6).is_none()
                && restricted.lookup(&8).is_none()
                && restricted.lookup(&10).is_none()
                && restricted.lookup(&12).is_none()
        );
        assert!(
            restricted.lookup(&1).is_some()
                && restricted.lookup(&3).is_some()
                && restricted.lookup(&5).is_some()
                && restricted.lookup(&7).is_some()
                && restricted.lookup(&9).is_some()
                && restricted.lookup(&11).is_some()
                && restricted.lookup(&13).is_some()
        );
        assert!(restricted.lookup(&1) == restricted.lookup(&3));
        assert!(
            restricted.lookup(&5) == restricted.lookup(&7)
                && restricted.lookup(&7) == restricted.lookup(&9)
        );
        assert!(restricted.lookup(&1) != restricted.lookup(&5));
        assert!(restricted.lookup(&1) != restricted.lookup(&11));
        assert!(restricted.lookup(&1) != restricted.lookup(&113));
        assert!(restricted.lookup(&5) != restricted.lookup(&11));
        assert!(restricted.lookup(&5) != restricted.lookup(&13));
        assert!(restricted.lookup(&11) != restricted.lookup(&13));
    }

    #[test]
    fn num_equivalences() {
        assert!(
            EqRel::<u32>::from("0 1 2 3 4 | 5 6 7 8 9| 10 11 | 12 13").num_equivalences() == 10
        );
        assert!(EqRel::<u32>::from("0 1 2 3 4 5 6 7 8 9 10 11 12 13").num_equivalences() == 13);
        assert!(EqRel::<u32>::from("0|1|2|3|4|5|6|7|8|9|10|11|12|13").num_equivalences() == 0);
    }
}
