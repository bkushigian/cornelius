use std::collections::{HashMap, HashSet};
use std::hash::Hash;
use std::str::FromStr;
use crate::subjects::AnalysisResult;

#[derive(Debug)]
/// Track an equality relation
pub struct EqRel<T: Eq + Hash> {
    /// A vec of the equivalence classes
    classes: Vec<HashSet<T>>,
    /// A lookup map from elements to the index of containing equivalence class
    /// in `classes`
    elem_map: HashMap<T, usize>
}

impl<T: Eq + Hash> EqRel<T> {
    /// Get a vec of equiv classes
    #[allow(dead_code)]
    pub fn classes(&self) -> &Vec<HashSet<T>> {
        &self.classes
    }

    /// Lookup an element
    pub fn lookup(&self, elem: &T) -> Option<&HashSet<T>> {
        self.classes.get(*self.elem_map.get(&elem)?)
    }

    /// Get the elements tracked by this `EqRel`
    pub fn elems(&self) -> HashSet<&T> {
        self.elem_map.keys().collect()
    }

    /// Check if this EqRel is a refinement of another EqRel. This means that:
    /// 1. self.elems().is_subset(&other.elems())
    /// 2. For each elem in self.elems(), self.lookup(elem).is_subset(other.lookup(elem))
    pub fn is_refinement_of(&self, other: &Self) -> bool {
        self.elems().is_subset(&other.elems()) &&
        self.elems().iter().all(|x| {
            let my_class = self.lookup(*x).expect("Error: EqRel has element unassociated with a class");
            let their_class = other.lookup(*x).expect("Error: EqRel has element unassociated with a class");
            my_class.is_subset(their_class)
        })
    }
}

impl<'a, T: FromStr + Eq + Hash + Copy> From<&str> for EqRel<T>
    where <T as std::str::FromStr>::Err : std::fmt::Debug {
    fn from(s: &str) -> Self {
        let mut classes = vec![];
        let mut elem_map = HashMap::default();
        for line in s.split('\n') {
            let mut class = HashSet::default();
            let idx = classes.len();
            for elem in line.split_whitespace() {
                let elem = elem.parse().unwrap_or_else(|_| panic!("Element {} is not a valid T", elem));
                class.insert(elem);
                elem_map.insert(elem, idx);
            }
            classes.push(class);
        }
        EqRel{classes, elem_map}
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
        EqRel{classes, elem_map}
    }
}


#[cfg(test)]
mod eq_rel {
    use super::EqRel;
    #[test]
    fn test_from_str() {
        let rel: EqRel<u32> = EqRel::from("1 2
3
4
5");
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
    }

    #[test]
    fn test_refines() {
        let course: EqRel<u32> = EqRel::from("1 2 3
4 5
6 7 8
9 10");
        let fine: EqRel<u32> = EqRel::from("1 2 3
4
5
6
7 8
9
10");
        assert!(fine.is_refinement_of(&course));
    }
}

pub mod io {
    use std::fs::File;
    use std::io::prelude::*;
    use std::io::Error;
    use itertools::Itertools;
    use crate::subjects::{Subject, Subjects};

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
        for subject in &subjects.subjects {
            write_subject_to_file(subject, dir)?;
            debug!("Successfully wrote subject {}", subject.method);
        }
        Ok(())
    }

    pub fn write_subject_to_file(subject: &Subject, _dir: &str) -> Result<(), Error> {
        let mut file_name = String::from(&subject.method);
        file_name.push_str(".equiv-class");
        let mut file = File::create(&file_name)?;
        info!("Writing subject to file {}", &file_name);
        let contents=get_equiv_file_contents_for_subject(subject);
        debug!("Contents:\n{}\n", &contents);
        file.write_all(contents.as_bytes())
    }

    fn get_equiv_file_contents_for_subject(subject: &Subject) -> String {
        let ar = &subject.analysis_result;
        let mut equiv_classes_as_strings = vec![];
        for equiv_class in &ar.equiv_classes {
            let equiv_class_as_string: String = itertools::sorted(equiv_class)
                .iter()
                .map(|id| (**id).to_string())
                .intersperse(" ".to_string())
                .collect();
            equiv_classes_as_strings.push(equiv_class_as_string);
        }
        equiv_classes_as_strings.join("\n")
    }
}
