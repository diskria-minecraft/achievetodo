import os
import json
import shutil
import argparse

def split_identifier(identifier):
    namespace, path = identifier.split(":")
    path_parts = path.split("/")
    advancement = path_parts[-1]
    tab = path_parts[-2] if len(path_parts) > 1 else ""
    return namespace, tab, advancement

def read_file_lines(file_path):
    with open(file_path, 'r', encoding='utf-8') as file:
        return file.readlines()

def find_first_matching_line(lines, substring):
    for line in lines:
        stripped_line = line.strip()
        if substring in stripped_line:
            return stripped_line[stripped_line.index(substring):].strip()
    return None

def extract_translate_values(json_content):
    translations = []
    if isinstance(json_content, dict):
        for key, value in json_content.items():
            if key == "translate" and ".minecraft." not in value:
                translations.append(value)
            else:
                translations.extend(extract_translate_values(value))
    elif isinstance(json_content, list):
        for item in json_content:
            translations.extend(extract_translate_values(item))
    return translations

parser = argparse.ArgumentParser(description="Process JSON files in the specified folder.")
parser.add_argument("bacap_path", help="Path to the BACAP folder.")
parser.add_argument("bacap_hardcore_path", help="Path to the BACAP Hardcore folder.")
parser.add_argument("bacap_terralith_path", help="Path to the BACAP Terralith folder.")
parser.add_argument("bacap_amplified_nether_path", help="Path to the BACAP Amplified Nether folder.")
parser.add_argument("bacap_nullscape_path", help="Path to the BACAP Nullscape folder.")
args = parser.parse_args()

bacap_path = args.bacap_path
bacap_hardcore_path = args.bacap_hardcore_path
bacap_terralith_path = args.bacap_terralith_path
bacap_amplified_nether_path = args.bacap_amplified_nether_path
bacap_nullscape_path = args.bacap_nullscape_path

if not os.path.exists(bacap_path):
    raise FileNotFoundError(f"Path '{bacap_path}' does not exist.")
if not os.path.exists(bacap_hardcore_path):
    raise FileNotFoundError(f"Path '{bacap_hardcore_path}' does not exist.")
if not os.path.exists(bacap_terralith_path):
    raise FileNotFoundError(f"Path '{bacap_terralith_path}' does not exist.")
if not os.path.exists(bacap_amplified_nether_path):
    raise FileNotFoundError(f"Path '{bacap_amplified_nether_path}' does not exist.")
if not os.path.exists(bacap_nullscape_path):
    raise FileNotFoundError(f"Path '{bacap_nullscape_path}' does not exist.")

bacap_override_msg_path = "src/main/resources/resourcepacks/bacap_override/data/bacap_rewards/function/msg"
bacap_hardcore_override_msg_path = "src/main/resources/resourcepacks/bacap_hardcore_override/data/bacap_rewards/function/msg"
bacap_terralith_override_msg_path = "src/main/resources/resourcepacks/bacap_terralith_override/data/bacap_rewards/function/msg"
bacap_amplified_nether_override_msg_path = "src/main/resources/resourcepacks/bacap_amplified_nether_override/data/bacap_rewards/function/msg"
bacap_nullscape_override_msg_path = "src/main/resources/resourcepacks/bacap_nullscape_override/data/bacap_rewards/function/msg"

if os.path.exists(bacap_override_msg_path):
    shutil.rmtree(bacap_override_msg_path)

if os.path.exists(bacap_hardcore_override_msg_path):
    shutil.rmtree(bacap_hardcore_override_msg_path)

if os.path.exists(bacap_terralith_override_msg_path):
    shutil.rmtree(bacap_terralith_override_msg_path)

if os.path.exists(bacap_amplified_nether_override_msg_path):
    shutil.rmtree(bacap_amplified_nether_override_msg_path)

if os.path.exists(bacap_nullscape_override_msg_path):
    shutil.rmtree(bacap_nullscape_override_msg_path)

for root, _, files in os.walk(bacap_path):
    for file in files:
        if file.endswith('.json'):
            json_file = os.path.join(root, file)
            with open(json_file, 'r', encoding='utf-8') as file:
                data = json.loads(file.read().replace("\\'", "\\\\'"))
                if 'display' in data and 'title' in data['display'] and 'rewards' in data and 'function' in data['rewards']:
                    reward_namespace, reward_tab, reward_advancement = split_identifier(data['rewards']['function'])
                    rewards_path = os.path.join(bacap_path, f"data/{reward_namespace}/function/{reward_tab}/{reward_advancement}.mcfunction")

                    if not os.path.exists(rewards_path):
                        continue

                    msg_function = find_first_matching_line(read_file_lines(rewards_path), "bacap_rewards:msg")
                    if msg_function:
                        msg_namespace, msg_tab, msg_advancement = split_identifier(msg_function)
                        bacap_msg_path = os.path.join(bacap_path, f"data/{msg_namespace}/function/msg/{msg_tab}/{msg_advancement}.mcfunction")

                        if not os.path.exists(bacap_msg_path):
                            continue

                        advancement_namespace = os.path.basename(os.path.dirname(os.path.dirname(os.path.dirname(json_file))))
                        advancement_tab = os.path.basename(os.path.dirname(json_file))
                        advancement_name = os.path.splitext(os.path.basename(json_file))[0]
                        injection = f'"clickEvent":{{"action":"run_command","value":"/advancementssearch highlight {advancement_namespace}:{advancement_tab}/{advancement_name} obtained_status"}},'
                        with open(bacap_msg_path, 'r', encoding='utf-8') as file:
                            overridden_msg = file.read().replace('"hoverEvent"', f'{injection}"hoverEvent"')

                        override_msg_function_path = os.path.join(bacap_override_msg_path, f"{msg_tab}/{msg_advancement}.mcfunction")
                        os.makedirs(os.path.dirname(override_msg_function_path), exist_ok=True)
                        with open(override_msg_function_path, 'w', encoding='utf-8') as file:
                            file.write(overridden_msg)

for root, _, files in os.walk(bacap_hardcore_path):
    for file in files:
        if file.endswith('.json'):
            json_file = os.path.join(root, file)
            with open(json_file, 'r', encoding='utf-8') as file:
                data = json.loads(file.read().replace("\\'", "\\\\'"))
                if 'display' in data and 'title' in data['display'] and 'rewards' in data and 'function' in data['rewards']:
                    advancement_namespace = os.path.basename(os.path.dirname(os.path.dirname(os.path.dirname(json_file))))
                    advancement_tab = os.path.basename(os.path.dirname(json_file))
                    advancement_name = os.path.splitext(os.path.basename(json_file))[0]
                    translate_values = extract_translate_values(data['display'])

                    with open(os.path.join(bacap_path, f"data/{advancement_namespace}/advancement/{advancement_tab}/{advancement_name}.json"), 'r', encoding='utf-8') as original_file:
                        original_data = json.loads(original_file.read().replace("\\'", "\\\\'"))
                        original_translate_values = extract_translate_values(original_data['display'])

                    original_description = None
                    hardcore_description = None

                    for i in range(len(original_translate_values)):
                        if original_translate_values[i] != translate_values[i]:
                            original_description = original_translate_values[i]
                            hardcore_description = translate_values[i]
                            break

                    if original_description:
                        reward_namespace, reward_tab, reward_advancement = split_identifier(data['rewards']['function'])
                        rewards_path = os.path.join(bacap_path, f"data/{reward_namespace}/function/{reward_tab}/{reward_advancement}.mcfunction")

                        if not os.path.exists(rewards_path):
                            continue

                        msg_function = find_first_matching_line(read_file_lines(rewards_path), "bacap_rewards:msg")
                        if msg_function:
                            _, msg_tab, msg_advancement = split_identifier(msg_function)
                            msg_path = os.path.join(bacap_override_msg_path, f"{msg_tab}/{msg_advancement}.mcfunction")

                            if not os.path.exists(msg_path):
                                continue

                            with open(msg_path, 'r', encoding='utf-8') as file:
                                hardcore_msg = file.read().replace(original_description, hardcore_description)

                            override_msg_function_path = os.path.join(bacap_hardcore_override_msg_path, f"{msg_tab}/{msg_advancement}.mcfunction")
                            os.makedirs(os.path.dirname(override_msg_function_path), exist_ok=True)
                            with open(override_msg_function_path, 'w', encoding='utf-8') as file:
                                file.write(hardcore_msg)

for root, _, files in os.walk(bacap_terralith_path):
    for file in files:
        if file.endswith('.json'):
            json_file = os.path.join(root, file)
            with open(json_file, 'r', encoding='utf-8') as file:
                data = json.loads(file.read().replace("\\'", "\\\\'"))
                if 'display' in data and 'title' in data['display'] and 'rewards' in data and 'function' in data['rewards']:
                    reward_namespace, reward_tab, reward_advancement = split_identifier(data['rewards']['function'])
                    rewards_path = os.path.join(bacap_terralith_path, f"data/{reward_namespace}/function/{reward_tab}/{reward_advancement}.mcfunction")

                    if not os.path.exists(rewards_path):
                        continue

                    msg_function = find_first_matching_line(read_file_lines(rewards_path), "bacap_rewards:msg")
                    if msg_function:
                        msg_namespace, msg_tab, msg_advancement = split_identifier(msg_function)
                        bacap_msg_path = os.path.join(bacap_terralith_path, f"data/{msg_namespace}/function/msg/{msg_tab}/{msg_advancement}.mcfunction")

                        if not os.path.exists(bacap_msg_path):
                            continue

                        advancement_namespace = os.path.basename(os.path.dirname(os.path.dirname(os.path.dirname(json_file))))
                        advancement_tab = os.path.basename(os.path.dirname(json_file))
                        advancement_name = os.path.splitext(os.path.basename(json_file))[0]
                        injection = f'"clickEvent":{{"action":"run_command","value":"/advancementssearch highlight {advancement_namespace}:{advancement_tab}/{advancement_name} obtained_status"}},'
                        with open(bacap_msg_path, 'r', encoding='utf-8') as file:
                            overridden_msg = file.read().replace('"hoverEvent"', f'{injection}"hoverEvent"')

                        override_msg_function_path = os.path.join(bacap_terralith_override_msg_path, f"{msg_tab}/{msg_advancement}.mcfunction")
                        os.makedirs(os.path.dirname(override_msg_function_path), exist_ok=True)
                        with open(override_msg_function_path, 'w', encoding='utf-8') as file:
                            file.write(overridden_msg)

for root, _, files in os.walk(bacap_amplified_nether_path):
    for file in files:
        if file.endswith('.json'):
            json_file = os.path.join(root, file)
            with open(json_file, 'r', encoding='utf-8') as file:
                data = json.loads(file.read().replace("\\'", "\\\\'"))
                if 'display' in data and 'title' in data['display'] and 'rewards' in data and 'function' in data['rewards']:
                    reward_namespace, reward_tab, reward_advancement = split_identifier(data['rewards']['function'])
                    rewards_path = os.path.join(bacap_path, f"data/{reward_namespace}/function/{reward_tab}/{reward_advancement}.mcfunction")

                    if not os.path.exists(rewards_path):
                        continue

                    msg_function = find_first_matching_line(read_file_lines(rewards_path), "bacap_rewards:msg")
                    if msg_function:
                        msg_namespace, msg_tab, msg_advancement = split_identifier(msg_function)
                        bacap_msg_path = os.path.join(bacap_amplified_nether_path, f"data/bc_rewards/function/msg/{msg_tab}/{msg_advancement}.mcfunction")

                        if not os.path.exists(bacap_msg_path):
                            continue

                        advancement_namespace = os.path.basename(os.path.dirname(os.path.dirname(os.path.dirname(json_file))))
                        advancement_tab = os.path.basename(os.path.dirname(json_file))
                        advancement_name = os.path.splitext(os.path.basename(json_file))[0]
                        injection = f'"clickEvent":{{"action":"run_command","value":"/advancementssearch highlight {advancement_namespace}:{advancement_tab}/{advancement_name} obtained_status"}},'
                        with open(bacap_msg_path, 'r', encoding='utf-8') as file:
                            overridden_msg = file.read().replace('"hoverEvent"', f'{injection}"hoverEvent"')

                        override_msg_function_path = os.path.join(bacap_amplified_nether_override_msg_path, f"{msg_tab}/{msg_advancement}.mcfunction")
                        os.makedirs(os.path.dirname(override_msg_function_path), exist_ok=True)
                        with open(override_msg_function_path, 'w', encoding='utf-8') as file:
                            file.write(overridden_msg)

for root, _, files in os.walk(bacap_nullscape_path):
    for file in files:
        if file.endswith('.json'):
            json_file = os.path.join(root, file)
            with open(json_file, 'r', encoding='utf-8') as file:
                data = json.loads(file.read().replace("\\'", "\\\\'"))
                if 'display' in data and 'title' in data['display'] and 'rewards' in data and 'function' in data['rewards']:
                    reward_namespace, reward_tab, reward_advancement = split_identifier(data['rewards']['function'])
                    rewards_path = os.path.join(bacap_nullscape_path, f"data/{reward_namespace}/function/{reward_tab}/{reward_advancement}.mcfunction")

                    if not os.path.exists(rewards_path):
                        continue

                    msg_function = find_first_matching_line(read_file_lines(rewards_path), "bacap_rewards:msg")
                    if msg_function:
                        msg_namespace, msg_tab, msg_advancement = split_identifier(msg_function)
                        bacap_msg_path = os.path.join(bacap_nullscape_path, f"data/{msg_namespace}/function/msg/{msg_tab}/{msg_advancement}.mcfunction")

                        if not os.path.exists(bacap_msg_path):
                            continue
                        advancement_namespace = os.path.basename(os.path.dirname(os.path.dirname(os.path.dirname(json_file))))
                        advancement_tab = os.path.basename(os.path.dirname(json_file))
                        advancement_name = os.path.splitext(os.path.basename(json_file))[0]
                        injection = f'"clickEvent":{{"action":"run_command","value":"/advancementssearch highlight {advancement_namespace}:{advancement_tab}/{advancement_name} obtained_status"}},'
                        with open(bacap_msg_path, 'r', encoding='utf-8') as file:
                            overridden_msg = file.read().replace('"hoverEvent"', f'{injection}"hoverEvent"')

                        override_msg_function_path = os.path.join(bacap_nullscape_override_msg_path, f"{msg_tab}/{msg_advancement}.mcfunction")
                        os.makedirs(os.path.dirname(override_msg_function_path), exist_ok=True)
                        with open(override_msg_function_path, 'w', encoding='utf-8') as file:
                            file.write(overridden_msg)
