# Function runs the first time BlazeandCave's Advancements Pack is loaded

scoreboard players set checking bac_settings 0
scoreboard players set adv_score bac_settings 0
scoreboard players set reward bac_settings 1
scoreboard players set exp bac_settings 1
scoreboard players set trophy bac_settings 1
scoreboard players set intro_msg bac_settings 1

scoreboard players set extra_reward bac_settings 0
scoreboard players set extra_trophy bac_settings 0

execute in the_end run gamerule announceAdvancements false
execute in overworld run gamerule announceAdvancements false
execute in the_nether run gamerule announceAdvancements false

execute unless score bac_created bac_created matches 1 run function blazeandcave:global_install
scoreboard players set bac_created bac_created 1

scoreboard objectives setdisplay sidebar bac_advancements
scoreboard objectives modify bac_advancements displayname {"translate":"gui.advancements"}

function blazeandcave:setup_item_rewards
function blazeandcave:setup_experience_rewards
function blazeandcave:setup_trophy_rewards
function blazeandcave:setup_cooperative_mode

scoreboard players set intro_msg bac_settings 0
scoreboard players set introduced bac_settings 1