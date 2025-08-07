import os, subprocess
'''
Por Favor 

run_challenge = "python run_challenge.py .  datasets/a output "
checher = "python checker.py datasets/a/instance_0001.txt output/instance_0001.txt"

'''

# Pasta com os arquivos de entrada
input_folder = input("Pasta: ").strip()
output_folder = "output"

# Caminho para o script checker
checker_script = "checker.py"

for filename in os.listdir(input_folder):
    if filename.endswith(".txt"):
        input_path = os.path.join(input_folder, filename)
        output_path = os.path.join(output_folder, filename)

        print(f"\n🧪 Testando {filename}")

        if not os.path.exists(output_path):
            print(f"⚠️ Arquivo de saída não encontrado: {output_path}")
            continue

        # Executa o checker.py com input e output
        result = subprocess.run(
            ["python", checker_script, input_path, output_path],
            capture_output=True,
            text=True
        )

        print(result.stdout)
        if result.stderr:
            print(f"⚠️ Erro:\n{result.stderr}")
