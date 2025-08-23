import json
import hashlib
from typing import Any, Optional
import redis
import os

class CacheManager:
    def __init__(self):
        self.redis_client = redis.Redis(
            host=os.getenv('REDIS_HOST', 'localhost'),
            port=int(os.getenv('REDIS_PORT', 6379)),
            password=os.getenv('REDIS_PASSWORD'),
            decode_responses=True
        )
        self.default_ttl = 3600  # 1 hour
    
    def _generate_key(self, prefix: str, data: Any) -> str:
        """Generate cache key from data"""
        data_str = json.dumps(data, sort_keys=True)
        hash_obj = hashlib.md5(data_str.encode())
        return f"{prefix}:{hash_obj.hexdigest()}"
    
    async def get(self, key: str) -> Optional[Any]:
        """Get value from cache"""
        try:
            value = self.redis_client.get(key)
            return json.loads(value) if value else None
        except:
            return None
    
    async def set(self, key: str, value: Any, ttl: int = None):
        """Set value in cache"""
        try:
            self.redis_client.setex(
                key,
                ttl or self.default_ttl,
                json.dumps(value)
            )
            return True
        except:
            return False
    
    async def delete(self, key: str):
        """Delete key from cache"""
        try:
            self.redis_client.delete(key)
            return True
        except:
            return False