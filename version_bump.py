import re

properties_file = "gradle.properties"
with open(properties_file, "r") as f:
    content = f.read()

match = re.search(r'projectVersion=K\.00\.(\d+)', content)
if match:
    current_version = int(match.group(1))
    new_version = f"projectVersion=K.00.{current_version + 1:03d}"
    content = content.replace(match.group(0), new_version)

    with open(properties_file, "w") as f:
        f.write(content)
    print(f"Bumped version to {new_version}")
else:
    print("Could not find projectVersion")
