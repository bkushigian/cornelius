/// Keep track of global runtime data across all subjects
use egg::StopReason;

#[derive(Default, Debug)]
pub struct GlobalData {
    pub iter_limits: u32,
    pub time_limits: u32,
    pub node_limits: u32,
    pub saturations: u32,
    pub others: u32,
    pub num_subjects: u32,
    pub num_subjects_files: u32,
    pub num_mutants: u32,
    pub discovered_equivalences: u32
}

impl GlobalData {
    pub fn handle_stop_reason(&mut self, stop_reason: &Option<StopReason>) {
        match stop_reason {
            Some(stop_reason) => {
                match stop_reason {
                    StopReason::Saturated => self.saturations += 1,
                    StopReason::IterationLimit(_) => self.iter_limits += 1,
                    StopReason::TimeLimit(_) => self.time_limits += 1,
                    StopReason::NodeLimit(_) => self.node_limits += 1,
                    StopReason::Other(_) => self.others += 1
                }
            }
            None => ()
        }
    }

    pub fn add_discovered_equivalences(&mut self, num_new_equivs: u32) {
        self.discovered_equivalences += num_new_equivs;
    }

    pub fn add_subjects(&mut self, num_new_subjects: u32) {
        self.num_subjects += num_new_subjects;
    }

    pub fn inc_subjects_files(&mut self) {
        self.num_subjects_files += 1;
    }

    pub fn add_mutants(&mut self, num_new_mutants: u32) {
        self.num_mutants += num_new_mutants;
    }
}

impl ToString for GlobalData {
    fn to_string(&self) -> String {
        format!("Stop Reasons
-------------
- iter limits reached: {}
- time limits reached: {}
- node limits reached: {}
- saturations:         {}
- other stop reasons:  {}

Subject Data
-------------
- num subject files:   {}
- num subjects:        {}
- num mutants:         {}
- num equivalences:    {}
",
            self.iter_limits,
            self.time_limits,
            self.node_limits,
            self.saturations,
            self.others,
            self.num_subjects_files,
            self.num_subjects,
            self.num_mutants,
            self.discovered_equivalences
        )
    }
}
