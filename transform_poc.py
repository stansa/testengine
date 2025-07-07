
import os
import shutil
import re
import argparse

def transform_poc(source_dir, target_dir, replacements):
    """
    Transform a POC directory by renaming entities and types.
    Args:
        source_dir: Source directory (e.g., 'car-engine-json')
        target_dir: Target directory (e.g., 'project-commodity-json')
        replacements: Dict of old_name -> new_name (e.g., {'engine': 'project', 'gas': 'project1'})
    """
    # Copy the source directory to the target
    if os.path.exists(target_dir):
        shutil.rmtree(target_dir)
    shutil.copytree(source_dir, target_dir)

    # Define regex patterns for replacements (case-sensitive, whole word)
    regex_replacements = [
        (r'\b' + old_name + r'\b', new_name)
        for old_name, new_name in replacements.items()
    ]
    # Add lowercase directory replacements
    regex_replacements.extend([
        (r'\b' + old_name.lower() + r's\b', new_name.lower() + 's')
        for old_name, new_name in replacements.items()
        if old_name in ['engine', 'car']
    ])
    # Add artifactId replacement
    artifact_id_old = r'car-engine-json'
    artifact_id_new = f"{replacements['engine'].lower()}-{replacements['car'].lower()}-json"
    regex_replacements.append((r'\b' + artifact_id_old + r'\b', artifact_id_new))
    # Replace EngineValidation
    regex_replacements.append((r'\bEngineValidation\b', 'ProjectValidation'))
    # Replace class names with package structure (e.g., engines.EngineGas -> projects.ProjectProject1)
    for old_name, new_name in replacements.items():
        if old_name in ['gas', 'electric', 'hybrid']:
            regex_replacements.append(
                (r'\bengines\.Engine' + old_name.capitalize() + r'\b', 
                 f'projects.Project{new_name.capitalize()}')
            )
        if old_name in ['sedan', 'suv']:
            regex_replacements.append(
                (r'\bcars\.Car' + old_name.capitalize() + r'\b', 
                 f'commodities.Commodity{new_name.capitalize()}')
            )
    # Replace package directories
    regex_replacements.append((r'\bengines\b', 'projects'))
    regex_replacements.append((r'\bcars\b', 'commodities'))

    # Walk through the target directory
    for root, dirs, files in os.walk(target_dir):
        # Rename directories
        for i, dir_name in enumerate(dirs):
            new_dir_name = dir_name
            for old_pattern, new_pattern in regex_replacements:
                new_dir_name = re.sub(old_pattern, new_pattern, new_dir_name)
            if new_dir_name != dir_name:
                os.rename(
                    os.path.join(root, dir_name),
                    os.path.join(root, new_dir_name)
                )
                dirs[i] = new_dir_name

        # Process files
        for file_name in files:
            old_path = os.path.join(root, file_name)
            # Rename files
            new_file_name = file_name
            for old_pattern, new_pattern in regex_replacements:
                new_file_name = re.sub(old_pattern, new_pattern, new_file_name)
            new_path = os.path.join(root, new_file_name)
            if new_file_name != file_name:
                os.rename(old_path, new_path)

            # Update file contents
            if new_path.endswith(('.java', '.xml', '.json')):
                with open(new_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                new_content = content
                for old_pattern, new_pattern in regex_replacements:
                    new_content = re.sub(old_pattern, new_pattern, new_content)
                if new_content != content:
                    with open(new_path, 'w', encoding='utf-8') as f:
                        f.write(new_content)

def main():
    parser = argparse.ArgumentParser(description="Transform POC entities and types")
    parser.add_argument("--source-dir", required=True, help="Source POC directory (e.g., car-engine-json)")
    parser.add_argument("--target-dir", required=True, help="Target POC directory (e.g., project-commodity-json)")
    parser.add_argument("replacements", nargs='*', help="Replacement pairs (e.g., engine=project car=commodity gas=project1)")
    
    args = parser.parse_args()
    replacements = {}
    for pair in args.replacements:
        if '=' not in pair:
            raise ValueError(f"Invalid replacement pair: {pair}")
        old_name, new_name = pair.split('=', 1)
        replacements[old_name] = new_name

    transform_poc(args.source_dir, args.target_dir, replacements)
    print(f"Transformed POC from {args.source_dir} to {args.target_dir}")

if __name__ == "__main__":
    main()