use crate::cli::CliArgs;
use instant::Duration;

pub struct RunConfig {
    pub iter_limit: usize,
    pub node_limit: usize,
    pub time_limit: Duration,
    pub verbose: bool,
    pub run_details: bool,
    pub iter_details: bool,
    pub write_equivs_to_single_file: bool,
    pub stop_on_error: bool,
}

impl Default for RunConfig {
    fn default() -> Self {
        RunConfig {
            iter_limit: 30,
            node_limit: 10_000,
            time_limit: Duration::from_secs(5),
            verbose: false,
            run_details: false,
            iter_details: false,
            write_equivs_to_single_file: true,
            stop_on_error: false,
        }
    }
}

impl RunConfig {
    pub fn with_iter_limit(mut self, iter_limit: usize) -> RunConfig {
        if iter_limit == 0 {
            self.iter_limit = usize::MAX;
        } else {
            self.iter_limit = iter_limit;
        }
        self
    }

    pub fn with_time_limit(mut self, time_limit: Duration) -> RunConfig {
        self.time_limit = time_limit;
        self
    }

    pub fn with_node_limit(mut self, node_limit: usize) -> RunConfig {
        if node_limit == 0 {
            self.node_limit = usize::MAX;
        } else {
            self.node_limit = node_limit;
        }
        self
    }

    pub fn with_verbosity(mut self, verbosity: bool) -> RunConfig {
        self.verbose = verbosity;
        self
    }

    pub fn with_run_details(mut self, run_details: bool) -> RunConfig {
        self.run_details = run_details;
        self
    }

    pub fn with_iter_details(mut self, iter_details: bool) -> RunConfig {
        self.iter_details = iter_details;
        self
    }

    pub fn with_write_to_single_file(mut self, write_equivs_to_single_file: bool) -> RunConfig {
        self.write_equivs_to_single_file = write_equivs_to_single_file;
        self
    }

    pub fn with_stop_on_error(mut self, stop_on_error: bool) -> RunConfig {
        self.stop_on_error = stop_on_error;
        self
    }
}

impl ToString for RunConfig {
    fn to_string(&self) -> String {
        format!(
            "max-iter-limit: {}
max-node-limit: {}
max-time-limit: {}
verbose: {}
run-details: {}
iter-details: {}
write-equivs-to-single-file: {}
stop-on-error: {}",

            self.iter_limit,
            self.node_limit,
            self.time_limit.as_secs(),
            self.verbose,
            self.run_details,
            self.iter_details,
            self.write_equivs_to_single_file,
            self.stop_on_error
        )
    }
}

impl From<CliArgs> for RunConfig {
    fn from(args: CliArgs) -> Self {
        RunConfig::default()
            .with_iter_limit(args.iter_limit)
            .with_node_limit(args.node_limit)
            .with_time_limit(Duration::from_secs(args.time_limit))
            .with_verbosity(args.verbose)
            .with_run_details(args.run_details)
            .with_iter_details(args.iter_details)
            .with_write_to_single_file(args.single_file_per_subjects)
            .with_stop_on_error(args.stop_on_error)
    }
}
