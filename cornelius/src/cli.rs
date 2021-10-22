use structopt::StructOpt;

#[derive(Debug, StructOpt)]
#[structopt(name="cornelius", about="Run equality saturation on Java mutants to detect mutant equivalence and redundancy")]
pub struct CliArgs {
    #[structopt(name="JAVA-FILES")]
    pub java_files: Vec<String>,

    #[structopt(long, default_value="30")]
    /// Maximum number of iterations
    pub iter_limit: usize,

    #[structopt(long, default_value="10000")]
    /// Maximum number of nodes
    pub node_limit: usize,

    #[structopt(long, default_value="5")]
    /// Time limit in seconds
    pub time_limit: u64,

    #[structopt(long, short)]
    /// Verbose output
    pub verbose: bool,

    #[structopt(long = "no-equiv-files")]
    /// Don't output equiv files
    pub suppress_equiv_file_output: bool,

    #[structopt(long)]
    /// Write a run-details file
    pub run_details: bool
}

impl Clone for CliArgs {
    fn clone(&self) -> Self {
        CliArgs {
            java_files: self.java_files.clone(),
            iter_limit: self.iter_limit,
            node_limit: self.node_limit,
            time_limit: self.time_limit,
            verbose: self.verbose,
            suppress_equiv_file_output: self.suppress_equiv_file_output,
            run_details: self.run_details
        }
    }
}
