# Android UI Automator MCP Server

A Model Context Protocol (MCP) server that provides Android UI automation capabilities through AI assistants.

## Architecture

```
AI Assistant (Claude, Cursor, etc.)
         ↕ JSON-RPC 2.0 (stdio)
    MCP Server (Python)
         ↕ HTTP
    Android Service (APK)
         ↕ UI Automator 2.0
    Android Device/Emulator
```

## Features

### 🔧 MCP Tools
- `get_ui_dump` - Get current UI hierarchy
- `click_element` - Click elements by various selectors
- `input_text` - Input text into fields
- `scroll_screen` - Scroll in any direction
- `wait_for_element` - Wait for elements to appear/disappear
- `press_back/home/recent` - Hardware button simulation
- `get_device_info` - Device specifications

### 📄 MCP Resources
- `android://ui/current` - Current UI state (JSON)
- `android://device/info` - Device information (JSON)

## Quick Start

### 1. Install Dependencies

```bash
cd mcp_server
pip install -r requirements.txt
```

### 2. Start Android Service

Install and run the Android APK on your device:

```bash
# Port forward (if using USB)
adb forward tcp:8080 tcp:8080

# Or use WiFi - find device IP
adb shell ip route | grep wlan0
```

### 3. Test MCP Server

```bash
# Test directly with stdio
python main.py --android-host 192.168.1.100 --android-port 8080 --stdio
```

### 4. Configure in AI Assistant

#### Claude Desktop

Add to `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "android-ui-automator": {
      "command": "python",
      "args": ["/path/to/mcp_server/main.py", "--android-host", "192.168.1.100"],
      "cwd": "/path/to/mcp_server"
    }
  }
}
```

#### VS Code with MCP Extension

Add to `.vscode/mcp.json`:

```json
{
  "servers": {
    "android-ui-automator": {
      "command": "python",
      "args": ["/path/to/mcp_server/main.py", "--android-host", "192.168.1.100"],
      "cwd": "/path/to/mcp_server"
    }
  }
}
```

#### Cursor

Add to settings:

```json
{
  "mcpServers": {
    "android-ui-automator": {
      "command": "python",
      "args": ["/path/to/mcp_server/main.py", "--android-host", "YOUR_DEVICE_IP"],
      "cwd": "/path/to/mcp_server"
    }
  }
}
```

## Usage Examples

### Get UI Information

```python
# AI Assistant can call:
get_ui_dump()

# Returns complete UI hierarchy with element properties:
# - resource_id, text, class_name, content_desc
# - bounds, clickable, scrollable, enabled states
# - nested children structure
```

### Click Elements

```python
# Click by resource ID (most reliable)
click_element({"resource_id": "com.example:id/login_button"})

# Click by text
click_element({"text": "Sign In"})

# Click by partial text
click_element({"text_contains": "Sign"})

# Click by class name
click_element({"class_name": "android.widget.Button"})
```

### Input Text

```python
# Input into a field
input_text({
    "selector": {"resource_id": "com.example:id/username"},
    "text": "john@example.com",
    "clear_first": True
})
```

### Scroll and Navigate

```python
# Scroll down
scroll_screen({"direction": "down", "steps": 3})

# Navigate with hardware buttons
press_back()
press_home()
press_recent()
```

### Wait for Elements

```python
# Wait for element to appear
wait_for_element({
    "selector": {"resource_id": "com.example:id/loading"},
    "condition": "visible",
    "timeout": 10000
})

# Wait for element to disappear
wait_for_element({
    "selector": {"text": "Loading..."},
    "condition": "gone"
})
```

## Configuration Options

```bash
python main.py --help

Options:
  --android-host HOST    Android device host/IP (default: localhost)
  --android-port PORT    Android service port (default: 8080)  
  --stdio               Use stdio transport (default)
  --debug               Enable debug logging
```

## Troubleshooting

### Connection Issues

1. **Check Android service status:**
   ```bash
   curl http://DEVICE_IP:8080/health
   ```

2. **Verify port forwarding:**
   ```bash
   adb forward --list
   ```

3. **Check firewall settings** on device and host

### MCP Integration Issues

1. **Check MCP server logs:**
   ```bash
   tail -f /tmp/mcp-ui-automator.log
   ```

2. **Test stdio communication:**
   ```bash
   echo '{"jsonrpc":"2.0","method":"tools/list","id":1}' | python main.py --stdio
   ```

3. **Validate configuration format** in your MCP client

### Element Selection Issues

1. **Use `get_ui_dump()` to inspect available elements**
2. **Prefer `resource_id` over text-based selectors**
3. **Check element properties** (clickable, enabled, visible)

## Advanced Usage

### Custom Android Service Configuration

If running Android service on different port:

```bash
python main.py --android-host 192.168.1.100 --android-port 9999
```

### Multiple Device Support

Run separate MCP server instances for each device:

```bash
# Device 1
python main.py --android-host 192.168.1.100 --android-port 8080

# Device 2  
python main.py --android-host 192.168.1.101 --android-port 8080
```

### Debug Mode

Enable detailed logging:

```bash
python main.py --debug --android-host 192.168.1.100
```

## Security Considerations

- **Android service runs on local network** - ensure network security
- **No authentication implemented** - use in trusted environments only
- **UI Automator permissions** - service can interact with all apps
- **Keep Android service updated** - monitor for security patches

## Contributing

1. Fork the repository
2. Create a feature branch
3. Test with multiple Android versions
4. Submit pull request with tests

## License

MIT License - see LICENSE file for details.
