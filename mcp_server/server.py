"""
Android UI Automator MCP Server Implementation

Provides MCP tools for Android UI automation through a standard MCP interface.
"""

import asyncio
import logging
from typing import Any, Dict, List, Optional, Sequence
import json

import mcp.types as types
from mcp.server import Server
from mcp.server.models import InitializationOptions
from mcp import ClientSession
from pydantic import AnyUrl

from android_client import AndroidClient

logger = logging.getLogger(__name__)

class UIAutomatorMCPServer:
    """MCP Server for Android UI Automation"""
    
    def __init__(self, android_host: str = "localhost", android_port: int = 8080):
        self.android_host = android_host
        self.android_port = android_port
        self.android_client = AndroidClient(android_host, android_port)
        
        # Initialize MCP server
        self.server = Server("android-ui-automator")
        self._setup_handlers()
        
        logger.info(f"UIAutomatorMCPServer initialized for {android_host}:{android_port}")
    
    def _setup_handlers(self):
        """Setup MCP server handlers"""
        
        @self.server.list_tools()
        async def handle_list_tools() -> List[types.Tool]:
            """List available UI automation tools"""
            return [
                types.Tool(
                    name="get_ui_dump",
                    description="Get the current UI hierarchy/tree from Android device",
                    inputSchema={
                        "type": "object",
                        "properties": {},
                        "required": []
                    }
                ),
                types.Tool(
                    name="click_element",
                    description="Click an element on the Android screen",
                    inputSchema={
                        "type": "object",
                        "properties": {
                            "resource_id": {
                                "type": "string",
                                "description": "Android resource ID (e.g., 'com.example:id/button')"
                            },
                            "text": {
                                "type": "string",
                                "description": "Exact text content to match"
                            },
                            "text_contains": {
                                "type": "string", 
                                "description": "Partial text content to match"
                            },
                            "class_name": {
                                "type": "string",
                                "description": "UI element class name (e.g., 'android.widget.Button')"
                            },
                            "content_desc": {
                                "type": "string",
                                "description": "Content description for accessibility"
                            },
                            "bounds": {
                                "type": "object",
                                "description": "Element bounds coordinates",
                                "properties": {
                                    "left": {"type": "integer"},
                                    "top": {"type": "integer"},
                                    "right": {"type": "integer"},
                                    "bottom": {"type": "integer"}
                                }
                            }
                        },
                        "anyOf": [
                            {"required": ["resource_id"]},
                            {"required": ["text"]},
                            {"required": ["text_contains"]},
                            {"required": ["class_name"]},
                            {"required": ["content_desc"]},
                            {"required": ["bounds"]}
                        ]
                    }
                ),
                types.Tool(
                    name="input_text",
                    description="Input text into a text field on Android device",
                    inputSchema={
                        "type": "object",
                        "properties": {
                            "text": {
                                "type": "string",
                                "description": "Text to input"
                            },
                            "clear_first": {
                                "type": "boolean",
                                "description": "Whether to clear existing text first",
                                "default": True
                            },
                            "selector": {
                                "type": "object",
                                "description": "Element selector for the input field",
                                "properties": {
                                    "resource_id": {"type": "string"},
                                    "text": {"type": "string"},
                                    "text_contains": {"type": "string"},
                                    "class_name": {"type": "string"},
                                    "content_desc": {"type": "string"}
                                }
                            }
                        },
                        "required": ["text", "selector"]
                    }
                ),
                types.Tool(
                    name="scroll_screen",
                    description="Scroll the screen in a specified direction",
                    inputSchema={
                        "type": "object",
                        "properties": {
                            "direction": {
                                "type": "string",
                                "enum": ["up", "down", "left", "right"],
                                "description": "Direction to scroll"
                            },
                            "steps": {
                                "type": "integer",
                                "description": "Number of scroll steps",
                                "default": 1,
                                "minimum": 1,
                                "maximum": 10
                            }
                        },
                        "required": ["direction"]
                    }
                ),
                types.Tool(
                    name="wait_for_element",
                    description="Wait for an element to appear or disappear",
                    inputSchema={
                        "type": "object",
                        "properties": {
                            "timeout": {
                                "type": "integer",
                                "description": "Timeout in milliseconds",
                                "default": 5000,
                                "minimum": 1000,
                                "maximum": 30000
                            },
                            "condition": {
                                "type": "string",
                                "enum": ["visible", "gone", "clickable"],
                                "description": "Wait condition",
                                "default": "visible"
                            },
                            "selector": {
                                "type": "object",
                                "description": "Element selector to wait for",
                                "properties": {
                                    "resource_id": {"type": "string"},
                                    "text": {"type": "string"},
                                    "text_contains": {"type": "string"},
                                    "class_name": {"type": "string"},
                                    "content_desc": {"type": "string"}
                                }
                            }
                        },
                        "required": ["selector"]
                    }
                ),
                types.Tool(
                    name="press_back",
                    description="Press the Android back button",
                    inputSchema={
                        "type": "object",
                        "properties": {},
                        "required": []
                    }
                ),
                types.Tool(
                    name="press_home",
                    description="Press the Android home button",
                    inputSchema={
                        "type": "object",
                        "properties": {},
                        "required": []
                    }
                ),
                types.Tool(
                    name="press_recent",
                    description="Press the Android recent apps button",
                    inputSchema={
                        "type": "object",
                        "properties": {},
                        "required": []
                    }
                ),
                types.Tool(
                    name="get_device_info",
                    description="Get Android device information",
                    inputSchema={
                        "type": "object",
                        "properties": {},
                        "required": []
                    }
                )
            ]
        
        @self.server.call_tool()
        async def handle_call_tool(name: str, arguments: Dict[str, Any]) -> List[types.TextContent]:
            """Handle tool calls"""
            logger.info(f"Tool called: {name} with args: {arguments}")
            
            try:
                if name == "get_ui_dump":
                    result = await self.android_client.get_ui_dump()
                    return [types.TextContent(
                        type="text",
                        text=json.dumps(result, indent=2, ensure_ascii=False)
                    )]
                
                elif name == "click_element":
                    # Build selector from arguments
                    selector = {k: v for k, v in arguments.items() if v is not None}
                    result = await self.android_client.click_element(selector)
                    return [types.TextContent(
                        type="text",
                        text=json.dumps(result, indent=2)
                    )]
                
                elif name == "input_text":
                    text = arguments["text"]
                    selector = arguments["selector"]
                    clear_first = arguments.get("clear_first", True)
                    
                    result = await self.android_client.input_text(selector, text, clear_first)
                    return [types.TextContent(
                        type="text",
                        text=json.dumps(result, indent=2)
                    )]
                
                elif name == "scroll_screen":
                    direction = arguments["direction"]
                    steps = arguments.get("steps", 1)
                    
                    result = await self.android_client.scroll(direction, steps)
                    return [types.TextContent(
                        type="text",
                        text=json.dumps(result, indent=2)
                    )]
                
                elif name == "wait_for_element":
                    selector = arguments["selector"]
                    timeout = arguments.get("timeout", 5000)
                    condition = arguments.get("condition", "visible")
                    
                    result = await self.android_client.wait_for_element(selector, timeout, condition)
                    return [types.TextContent(
                        type="text", 
                        text=json.dumps(result, indent=2)
                    )]
                
                elif name == "press_back":
                    result = await self.android_client.press_back()
                    return [types.TextContent(
                        type="text",
                        text=json.dumps(result, indent=2)
                    )]
                
                elif name == "press_home":
                    result = await self.android_client.press_home()
                    return [types.TextContent(
                        type="text",
                        text=json.dumps(result, indent=2)
                    )]
                
                elif name == "press_recent":
                    result = await self.android_client.press_recent()
                    return [types.TextContent(
                        type="text",
                        text=json.dumps(result, indent=2)
                    )]
                
                elif name == "get_device_info":
                    result = await self.android_client.get_device_info()
                    return [types.TextContent(
                        type="text",
                        text=json.dumps(result, indent=2)
                    )]
                
                else:
                    raise ValueError(f"Unknown tool: {name}")
                    
            except Exception as e:
                logger.error(f"Error executing tool {name}: {e}", exc_info=True)
                return [types.TextContent(
                    type="text",
                    text=json.dumps({
                        "success": False,
                        "error": str(e),
                        "error_type": type(e).__name__
                    }, indent=2)
                )]
        
        @self.server.list_resources()
        async def handle_list_resources() -> List[types.Resource]:
            """List available resources"""
            return [
                types.Resource(
                    uri=AnyUrl("android://ui/current"),
                    name="Current UI State",
                    description="Current Android UI hierarchy and element information",
                    mimeType="application/json"
                ),
                types.Resource(
                    uri=AnyUrl("android://device/info"),
                    name="Device Information",
                    description="Android device specifications and status",
                    mimeType="application/json"
                )
            ]
        
        @self.server.read_resource()
        async def handle_read_resource(uri: AnyUrl) -> str:
            """Handle resource reading"""
            logger.info(f"Resource requested: {uri}")
            
            if str(uri) == "android://ui/current":
                result = await self.android_client.get_ui_dump()
                return json.dumps(result, indent=2, ensure_ascii=False)
            
            elif str(uri) == "android://device/info":
                result = await self.android_client.get_device_info()
                return json.dumps(result, indent=2)
            
            else:
                raise ValueError(f"Unknown resource: {uri}")
    
    async def run_stdio(self, read_stream, write_stream):
        """Run the MCP server with stdio transport"""
        logger.info("MCP server running with stdio transport")
        
        async with ClientSession(read_stream, write_stream) as session:
            # Initialize the session
            await session.initialize(
                InitializationOptions(
                    server_name="android-ui-automator",
                    server_version="1.0.0",
                    capabilities=session.get_server_capabilities(
                        notification_options=None,
                        experimental_capabilities={}
                    )
                )
            )
            
            # Keep the server running
            await session.run_server()
