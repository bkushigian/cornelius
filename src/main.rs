pub mod subjects;
pub mod delta;
pub mod peg;
pub mod rewrites;
mod tests;

use crate::subjects::run_on_subjects_file;
use std::env;

#[macro_use] extern crate log;

fn main() -> Result<(), String> {
    let _ = env_logger::builder().try_init();
    let args: Vec<String> = env::args().collect();
    let subject_files = &args[1..];

    for subj_file in subject_files {
        let _ = run_on_subjects_file(subj_file);
    }
    Ok(())
}
