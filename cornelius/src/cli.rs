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
    pub suppress_equiv_file_output: bool
}
