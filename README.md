
# Memories

Minecraft Forge mod that takes screenshots periodically and uploads them to a server.


This is made for small modpack communities who want to keep track and have a retrospect of the progress of the players.

## Documentation

The mod uploads screenshots with an HTTP POST request to the URL in your configuration file `memories-client.toml`, along with the username of the user, in the following format:

```json
{
    "username": "The username of the player",
    "screenshot": "data:image/jpeg;base64,..."
}
```


## License

[MIT](https://choosealicense.com/licenses/mit/)
