"""
Android HTTP Client

This module provides an HTTP client to communicate with the Android UI Automator service.
"""

import asyncio
import aiohttp
import logging
from typing import Any, Dict, Optional
import json

logger = logging.getLogger(__name__)

class AndroidClient:
    """HTTP client for Android UI Automator service"""
    
    def __init__(self, host: str = "localhost", port: int = 8080):
        self.host = host
        self.port = port
        self.base_url = f"http://{host}:{port}"
        self.session: Optional[aiohttp.ClientSession] = None
        
        logger.info(f"AndroidClient initialized for {self.base_url}")
    
    async def _get_session(self) -> aiohttp.ClientSession:
        """Get or create aiohttp session"""
        if self.session is None or self.session.closed:
            self.session = aiohttp.ClientSession(
                timeout=aiohttp.ClientTimeout(total=30),
                headers={"Content-Type": "application/json"}
            )
        return self.session
    
    async def close(self):
        """Close the HTTP session"""
        if self.session and not self.session.closed:
            await self.session.close()
    
    async def _make_request(self, method: str, endpoint: str, data: Optional[Dict] = None) -> Dict[str, Any]:
        """Make HTTP request to Android service"""
        url = f"{self.base_url}{endpoint}"
        session = await self._get_session()
        
        try:
            logger.debug(f"{method} {url} with data: {data}")
            
            async with session.request(method, url, json=data) as response:
                if response.content_type == 'application/json':
                    result = await response.json()
                else:
                    # Handle text responses
                    text = await response.text()
                    result = {"content": text}
                
                logger.debug(f"Response: {result}")
                
                if response.status >= 400:
                    logger.error(f"HTTP {response.status}: {result}")
                    result["http_status"] = response.status
                
                return result
                
        except aiohttp.ClientError as e:
            logger.error(f"HTTP client error: {e}")
            return {
                "success": False,
                "error": f"HTTP client error: {str(e)}",
                "error_type": "client_error"
            }
        except Exception as e:
            logger.error(f"Unexpected error: {e}", exc_info=True)
            return {
                "success": False,
                "error": f"Unexpected error: {str(e)}",
                "error_type": "unknown_error"
            }
    
    async def health_check(self) -> Dict[str, Any]:
        """Check if Android service is healthy"""
        return await self._make_request("GET", "/health")
    
    async def get_ui_dump(self) -> Dict[str, Any]:
        """Get current UI hierarchy"""
        return await self._make_request("GET", "/ui/dump")
    
    async def get_ui_dump_xml(self) -> Dict[str, Any]:
        """Get current UI hierarchy in XML format"""
        return await self._make_request("GET", "/ui/dump/xml")
    
    async def click_element(self, selector: Dict[str, Any]) -> Dict[str, Any]:
        """Click an element using selector"""
        return await self._make_request("POST", "/ui/click", {"selector": selector})
    
    async def input_text(self, selector: Dict[str, Any], text: str, clear_first: bool = True) -> Dict[str, Any]:
        """Input text into an element"""
        data = {
            "selector": selector,
            "text": text,
            "clear_first": clear_first
        }
        return await self._make_request("POST", "/ui/input", data)
    
    async def scroll(self, direction: str, steps: int = 1, selector: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Scroll the screen or a specific element"""
        data = {
            "direction": direction,
            "steps": steps
        }
        if selector:
            data["selector"] = selector
        
        return await self._make_request("POST", "/ui/scroll", data)
    
    async def wait_for_element(self, selector: Dict[str, Any], timeout: int = 5000, condition: str = "visible") -> Dict[str, Any]:
        """Wait for an element to meet a condition"""
        data = {
            "selector": selector,
            "timeout": timeout,
            "condition": condition
        }
        return await self._make_request("POST", "/ui/wait", data)
    
    async def press_back(self) -> Dict[str, Any]:
        """Press back button"""
        return await self._make_request("POST", "/device/back")
    
    async def press_home(self) -> Dict[str, Any]:
        """Press home button"""
        return await self._make_request("POST", "/device/home")
    
    async def press_recent(self) -> Dict[str, Any]:
        """Press recent apps button"""
        return await self._make_request("POST", "/device/recent")
    
    async def get_device_info(self) -> Dict[str, Any]:
        """Get device information"""
        return await self._make_request("GET", "/device/info")
    
    def __del__(self):
        """Cleanup when object is destroyed"""
        if self.session and not self.session.closed:
            # Try to schedule cleanup, but don't fail if event loop is already closed
            try:
                loop = asyncio.get_event_loop()
                if loop.is_running():
                    loop.create_task(self.close())
            except RuntimeError:
                pass  # Event loop is already closed
