use cornelius::subjects::{run_on_subjects_file};
use cornelius::util::io::{write_subjects_to_separate_files, write_run_details_to_file};
use instant::Duration;
use cornelius::cli::CliArgs;
use cornelius::config::RunConfig;
use cornelius::global_data::GlobalData;
use structopt::StructOpt;

fn main() -> Result<(), String> {
    let _ = env_logger::builder().try_init();
    let args = CliArgs::from_args();

    let mut total_equivs_found = 0;
    let mut total_mutants_found = 0;
    let config = RunConfig::default()
        .with_iter_limit(args.iter_limit)
        .with_node_limit(args.node_limit)
        .with_time_limit(Duration::from_secs(args.time_limit))
        .with_verbosity(args.verbose);
    let mut global_data = GlobalData::default();

    for subj_file in &args.java_files {
        if args.verbose {
            println!("{}:", &subj_file);
        }
        match run_on_subjects_file(&subj_file, &config, &mut global_data) {
            Ok(subjects) => {
                let mut found = 0;
                total_mutants_found += subjects.num_mutants();
                for subj in &subjects.subjects {
                    found += subj.analysis_result.score;
                }
                if found > 0 {
                    total_equivs_found += found;
                    if args.verbose {
                        println!("--------------------------------------------------------------------------------");
                        println!("{}\n", &subj_file);
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
                }
                if ! args.suppress_equiv_file_output {
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
            }
            Err(msg) =>
                println!("Error running on subject file {}:\n    {}", subj_file, msg),
        };
    }
    if args.java_files.len() > 1 {
        println!("        SUMMARY");
        println!("        =======");

        println!("Mutants found: {}", total_mutants_found);
        println!("Equivs found: {}", total_equivs_found);
    }
    write_run_details_to_file(&config, &global_data, "run.details").map_err(|e| e.to_string())
}
