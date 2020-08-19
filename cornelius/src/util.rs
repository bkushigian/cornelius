use std::collections::{HashMap, HashSet};
use std::hash::Hash;
use std::str::FromStr;
use crate::subjects::AnalysisResult;

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
            let idx = classes.len() - 1;
            for elem in line.split(' ') {
                let elem = elem.parse().unwrap();
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
