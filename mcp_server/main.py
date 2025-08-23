#!/usr/bin/env python3
"""
Android UI Automator MCP Server

This MCP server provides Android UI automation tools through the Model Context Protocol.
It communicates with an Android device running the UI Automator HTTP service.
"""

import asyncio
import json
import sys
from typing import Optional
import argparse
import logging

from mcp import ClientSession, StdioServerParameters
from mcp.server import Server
from mcp.server.stdio import stdio_server

from server import UIAutomatorMCPServer

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('/tmp/mcp-ui-automator.log'),
        logging.StreamHandler(sys.stderr)
    ]
)
logger = logging.getLogger(__name__)

async def main():
    """Main entry point for the MCP server"""
    parser = argparse.ArgumentParser(description='Android UI Automator MCP Server')
    parser.add_argument(
        '--android-host', 
        type=str, 
        default='localhost',
        help='Android device host/IP address (default: localhost)'
    )
    parser.add_argument(
        '--android-port', 
        type=int, 
        default=8080,
        help='Android service port (default: 8080)'
    )
    parser.add_argument(
        '--stdio', 
        action='store_true', 
        help='Use stdio transport (default)'
    )
    parser.add_argument(
        '--debug', 
        action='store_true',
        help='Enable debug logging'
    )
    
    # Parse arguments or use stdio by default
    if len(sys.argv) == 1:
        # Default to stdio mode if no arguments provided
        args = parser.parse_args(['--stdio'])
    else:
        args = parser.parse_args()
    
    if args.debug:
        logging.getLogger().setLevel(logging.DEBUG)
        logger.debug("Debug logging enabled")
    
    try:
        # Create the MCP server instance
        logger.info(f"Initializing Android UI Automator MCP Server")
        logger.info(f"Android service: http://{args.android_host}:{args.android_port}")
        
        mcp_server = UIAutomatorMCPServer(
            android_host=args.android_host,
            android_port=args.android_port
        )
        
        if args.stdio:
            logger.info("Starting MCP server with stdio transport...")
            
            # Run the stdio server
            async with stdio_server() as (read_stream, write_stream):
                await mcp_server.run_stdio(read_stream, write_stream)
        else:
            logger.error("Only stdio transport is currently supported")
            sys.exit(1)
            
    except KeyboardInterrupt:
        logger.info("Server interrupted by user")
    except Exception as e:
        logger.error(f"Server error: {e}", exc_info=True)
        sys.exit(1)

if __name__ == "__main__":
    asyncio.run(main())
