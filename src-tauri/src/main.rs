use std::path::PathBuf;
use std::process::Command;

fn repo_root() -> Result<PathBuf, String> {
    let current = std::env::current_dir().map_err(|e| format!("cwd invalido: {e}"))?;
    if current.ends_with("src-tauri") {
        return current
            .parent()
            .map(|p| p.to_path_buf())
            .ok_or_else(|| "nao foi possivel resolver raiz do projeto".to_string());
    }
    Ok(current)
}

fn run_stack_script(script_name: &str) -> Result<(), String> {
    let root = repo_root()?;
    let script_path = root.join("desktop").join(script_name);

    if !script_path.exists() {
        return Err(format!("script nao encontrado: {}", script_path.display()));
    }

    let status = Command::new("bash")
        .arg(script_path.as_os_str())
        .current_dir(root)
        .status()
        .map_err(|e| format!("falha ao executar {script_name}: {e}"))?;

    if !status.success() {
        return Err(format!("{script_name} finalizou com erro ({status})"));
    }

    Ok(())
}

fn main() {
    let app = tauri::Builder::default().setup(|_app| {
        run_stack_script("run-desktop-stack.sh")
            .map_err(|e| -> Box<dyn std::error::Error> { e.into() })?;
        Ok(())
    });

    app.build(tauri::generate_context!())
        .expect("erro ao criar app tauri")
        .run(|_app_handle, event| {
            if matches!(event, tauri::RunEvent::ExitRequested { .. }) {
                if let Err(err) = run_stack_script("stop-desktop-stack.sh") {
                    eprintln!("falha ao encerrar stack local: {err}");
                }
            }
        });
}
