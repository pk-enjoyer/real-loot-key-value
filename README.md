![image](https://img.shields.io/endpoint?url=https://api.runelite.net/pluginhub/shields/installs/plugin/real-loot-key-value)
# Real Loot Key Value

Real Loot Key Value shows exact, configurable Grand Exchange values for PvP loot keys directly in the loot chest interface.

The Old School RuneScape loot key chest can round the displayed value down to the nearest million. That makes the value look lower than it really is, sometimes by a large amount. A key worth 1.9M, for example, can appear much closer to 1M in the default interface.

This plugin fixes that UI problem by overlaying a more accurate value in the loot key chest. Coin stacks are counted as their exact GP amount, and item stacks can be valued with either RuneLite's actively traded Grand Exchange price or Jagex's Grand Exchange guide price.

The **Price source** setting defaults to **RuneLite actively traded** and is independent of RuneLite's global item-price preference. Select **Jagex guide price** to use the Jagex price field intended to match in-game loot-key values such as key checks and clan loot messages. Both sources come from RuneLite's existing item-price cache, so the plugin does not need an examine action, chat-message parsing, or extra network requests.

Display options can be combined. By default the plugin keeps a key icon on the tab and shows the exact value at the bottom of the chest, with optional compact tab values, top-right text, and high-value highlighting.

The plugin is display-only. It does not change loot, add actions, click anything for you, store data, or send any information anywhere.
*Before*:

<img width="300" height="310" alt="Screenshot From 2026-06-27 00-09-12 tmp" src="https://github.com/user-attachments/assets/3a950abf-4c6f-4da1-bb5f-0e7fe68ba168" />

| After                                                                                                                                                            | After                                                                                                                                                            | After                                                                                                                                                            |
|------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Was "1m" but actual value is 1.7m: | Other example, only bottom text: | Another example, with Resource packs <br />  and no bottom text:|
| <img width="300" height="310" alt="Screenshot From 2026-06-27 00-08-54 tmp" src="https://github.com/user-attachments/assets/97abc69b-0de2-41ce-8f62-ee31bb3d382c" />| <img width="300" height="310" alt="image" src="https://github.com/user-attachments/assets/3d6253f4-87ae-45f7-a84f-8b9bc97bae8e" /> | <img width="300" height="310" alt="95BC557D-CC20-4CAF-92B0-FCE29029415C tmp" src="https://github.com/user-attachments/assets/e627a9b3-7284-43f9-ba54-6957fb4a00fc" /> |
