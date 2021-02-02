use cornelius::subjects::{run_on_subjects_file};
use cornelius::util::io::{write_subjects_to_separate_files};
use std::env;

fn main() -> Result<(), String> {
    let _ = env_logger::builder().try_init();
    let args: Vec<String> = env::args().collect();
    let subject_files = &args[1..];
    let mut total_equivs_found = 0;

    for subj_file in subject_files {
        match run_on_subjects_file(subj_file) {
            Ok(subjects) => {
                let mut found = 0;
                for subj in &subjects.subjects {
                    found += subj.analysis_result.score;
                }
                if found > 0 {
                    total_equivs_found += found;
                    println!("--------------------------------------------------------------------------------");
                    println!("{}\n", subj_file);
                    println!("Found {} equivalences:", found);
                    for subj in &subjects.subjects {
                        if subj.analysis_result.score > 0 {
                            println!("- {}: {}", subj.method, subj.analysis_result.score);
                            for eq_class in &subj.analysis_result.equiv_classes {
                                if eq_class.len() > 1 {
                                    print!("  ");
                                    for x in eq_class {
                                        print!(" {  }", x);
                                    }
                                    print!("\n");
                                }
                            }
                        }
                    }
                }
                let mut equiv_file = String::from(subj_file);
                equiv_file.push_str(".equiv-class");

                match  write_subjects_to_separate_files(&subjects, ".") {
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
    if subject_files.len() > 1 {
        println!("        SUMMARY");
        println!("        =======");
        println!("Equivs found: {}", total_equivs_found);
    }
    Ok(())
}
