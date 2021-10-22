use instant::Duration;

pub struct RunConfig {
    pub iter_limit: usize,
    pub node_limit: usize,
    pub time_limit: Duration,
    pub verbose: bool
}

impl Default for RunConfig {
    fn default() -> Self {
        RunConfig {
            iter_limit: 30,
            node_limit: 10_000,
            time_limit: Duration::from_secs(5),
            verbose: false
        }
    }
}

impl RunConfig {
    pub fn with_iter_limit(mut self, iter_limit: usize) -> RunConfig {
        self.iter_limit = iter_limit;
        self
    }

    pub fn with_time_limit(mut self, time_limit: Duration) -> RunConfig {
        self.time_limit = time_limit;
        self
    }

    pub fn with_node_limit(mut self, node_limit: usize) -> RunConfig {
        self.node_limit = node_limit;
        self
    }

    pub fn with_verbosity(mut self, verbosity: bool) -> RunConfig {
        self.verbose = verbosity;
        self
    }
}

impl ToString for RunConfig {
    fn to_string(&self) -> String {
        format!("- Iteration Limit: {}
- Node Limit: {}
- Time Limit: {}
- Verbose:    {}
", self.iter_limit, self.node_limit, self.time_limit.as_secs(), self.verbose)
    }
}
