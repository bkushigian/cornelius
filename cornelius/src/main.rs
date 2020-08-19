pub mod subjects;
pub mod delta;
pub mod peg;
pub mod rewrites;
mod tests;
mod util;

use crate::subjects::{run_on_subjects_file, write_results_to_file};
use std::env;

#[macro_use] extern crate log;

fn main() -> Result<(), String> {
    let _ = env_logger::builder().try_init();
    let args: Vec<String> = env::args().collect();
    let subject_files = &args[1..];

    for subj_file in subject_files {
        match run_on_subjects_file(subj_file) {
            Ok(subjects) => {
                let mut equiv_file = String::from(subj_file);
                equiv_file.push_str(".equiv-class");

                match  write_results_to_file(&subjects, equiv_file.as_str()) {
                    Err(e) => {
                        println!("Error writing results of subject file {} to equiv-class file {}.",
                            subj_file, equiv_file);
                        println!("    {}", e);
                    }
                    Ok(()) => (),
                }
            }
            Err(msg) =>
                println!("Error running on subject file {}:\n    {}", subj_file, msg),
        };
    }
    Ok(())
}
