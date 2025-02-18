# BlockOwner

BlockOwner is a simple Minecraft mod that allows you to track and view the owner of each player-placed block. This mod requires operator permissions to function and works entirely server-side, so there's no need for any client-side installation or configuration.

# Preview
![Preview](https://raw.githubusercontent.com/mastercion/minecraft_mod_fabric_blockowner/a52745b22d01ccf70d060d7c13c87ea1cbb6bcc9/media/gif/preview.gif) ![Command](https://raw.githubusercontent.com/mastercion/minecraft_mod_fabric_blockowner/refs/heads/media/media/gif/list_command.gif)

## FAQ
### **How does it work?**
It tracks each block placed by every player on the server and stores them on a per-player basis to be later viewed using the selection tool.

## Features

- **View Block Owners**: Easily track and identify who placed a block by using a wooden hoe or your customized tool.
- **Permissions**: Only players with operator permissions can use this feature, ensuring secure usage.
- **Server-Side Only**: No need for any client-side installation, making it hassle-free to set up and use.
- **Commands**:
  - **`/blockowner level [none|minimal|all]`**
  
  Adjusts the log level for the mod, controlling the verbosity of logs.
  - **`/blockowner tool minecraft:item_name`**
  
  Sets the tool used for inspecting blocks. The selected tool is saved in the configuration file for persistent use.
  - **`/blockowner style &1-9{Date} &1-9{Player} &1-9{Block}`**
  
  Allows you to customize the Message of the inspect tool in the way you want it. You can change the order of {Date} {Player} {Block} any way you want and assign minecraft color codes to them.
  - **`/blockowner visualize [playername|clear] range(min10)`**
  
  Allows you to visualize the Blocks placed by the Person with extra info. (currenty only in TEST builds)

**Feel free to submit your ideas!**

**To-Do list**: [Open issue Page](https://github.com/mastercion/minecraft_mod_fabric_blockowner/issues/1)

## Setup

1. **Download the Mod**: [Download the latest version](https://github.com/mastercion/minecraft_mod_fabric_blockowner/releases)
2. **Install**:
   - Place the mod into your `Server/mods` folder.
   - No further configuration needed.
3. **Start Your Server**: Run your Minecraft server and enjoy!

## Usage

1. **User with Operator**: Ensure you have operator permissions on your server.
2. **Use the Selection Tool**: Right-click with a wooden hoe or your configured tool to view the owner of the block you're looking at.
3. **Commands**:
   - **/blockowner level [none|minimal|all]**: Adjust the log level.
   - **/blockowner tool [ANYTOOL]**: Set the tool for inspecting blocks.

## License

This mod is available under the CC0 license. Feel free to learn from it and incorporate it into your own projects.
