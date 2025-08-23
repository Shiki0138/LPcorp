// Simplified Redis implementation using Map for development

class SimpleRedis {
  private store = new Map<string, { value: string; expires?: number }>();
  private keyExpiration = new Map<string, NodeJS.Timeout>();

  async set(key: string, value: string, mode?: string, duration?: number): Promise<'OK'> {
    // Clear existing expiration if any
    if (this.keyExpiration.has(key)) {
      clearTimeout(this.keyExpiration.get(key)!);
      this.keyExpiration.delete(key);
    }

    const expires = mode === 'EX' && duration ? Date.now() + (duration * 1000) : undefined;
    this.store.set(key, { value, expires });

    // Set expiration timer
    if (expires) {
      const timeout = setTimeout(() => {
        this.store.delete(key);
        this.keyExpiration.delete(key);
      }, duration! * 1000);
      this.keyExpiration.set(key, timeout);
    }

    return 'OK';
  }

  async get(key: string): Promise<string | null> {
    const item = this.store.get(key);
    if (!item) return null;
    
    if (item.expires && Date.now() > item.expires) {
      this.store.delete(key);
      if (this.keyExpiration.has(key)) {
        clearTimeout(this.keyExpiration.get(key)!);
        this.keyExpiration.delete(key);
      }
      return null;
    }
    
    return item.value;
  }

  async del(...keys: string[]): Promise<number> {
    let count = 0;
    for (const key of keys) {
      const existed = this.store.delete(key);
      if (this.keyExpiration.has(key)) {
        clearTimeout(this.keyExpiration.get(key)!);
        this.keyExpiration.delete(key);
      }
      if (existed) count++;
    }
    return count;
  }

  async exists(key: string): Promise<number> {
    const item = this.store.get(key);
    if (!item) return 0;
    
    if (item.expires && Date.now() > item.expires) {
      this.store.delete(key);
      if (this.keyExpiration.has(key)) {
        clearTimeout(this.keyExpiration.get(key)!);
        this.keyExpiration.delete(key);
      }
      return 0;
    }
    
    return 1;
  }

  async ttl(key: string): Promise<number> {
    const item = this.store.get(key);
    if (!item) return -2;
    if (!item.expires) return -1;
    
    const remaining = Math.ceil((item.expires - Date.now()) / 1000);
    return remaining > 0 ? remaining : -2;
  }

  async disconnect(): Promise<void> {
    // Clear all timeouts
    for (const timeout of this.keyExpiration.values()) {
      clearTimeout(timeout);
    }
    this.keyExpiration.clear();
    this.store.clear();
  }

  // Additional methods for compatibility
  async expire(key: string, seconds: number): Promise<number> {
    const item = this.store.get(key);
    if (!item) return 0;

    // Clear existing expiration
    if (this.keyExpiration.has(key)) {
      clearTimeout(this.keyExpiration.get(key)!);
      this.keyExpiration.delete(key);
    }

    // Set new expiration
    const expires = Date.now() + (seconds * 1000);
    this.store.set(key, { ...item, expires });

    const timeout = setTimeout(() => {
      this.store.delete(key);
      this.keyExpiration.delete(key);
    }, seconds * 1000);
    this.keyExpiration.set(key, timeout);

    return 1;
  }

  async keys(pattern: string): Promise<string[]> {
    const allKeys = Array.from(this.store.keys());
    if (pattern === '*') return allKeys;
    
    // Simple pattern matching
    const regex = new RegExp(pattern.replace(/\*/g, '.*'));
    return allKeys.filter(key => regex.test(key));
  }

  // Extended Redis functionality for compatibility
  async setex(key: string, seconds: number, value: string): Promise<'OK'> {
    await this.set(key, value, 'EX', seconds);
    return 'OK';
  }

  async ping(): Promise<'PONG'> {
    return 'PONG';
  }

  async flushdb(): Promise<'OK'> {
    this.store.clear();
    for (const timeout of this.keyExpiration.values()) {
      clearTimeout(timeout);
    }
    this.keyExpiration.clear();
    return 'OK';
  }

  async dbsize(): Promise<number> {
    return this.store.size;
  }

  async info(section?: string): Promise<string> {
    const memoryUsage = process.memoryUsage();
    return `used_memory:${memoryUsage.heapUsed}\nused_memory_human:${Math.round(memoryUsage.heapUsed / 1024 / 1024)}M`;
  }

  async memory(command: string, key?: string): Promise<number> {
    if (command === 'usage') {
      const item = this.store.get(key || '');
      return item ? JSON.stringify(item).length : 0;
    }
    return 0;
  }

  // Hash operations
  private hashStore = new Map<string, Map<string, string>>();

  async hset(key: string, field: string, value: string): Promise<number> {
    if (!this.hashStore.has(key)) {
      this.hashStore.set(key, new Map());
    }
    const hash = this.hashStore.get(key)!;
    const isNew = !hash.has(field);
    hash.set(field, value);
    return isNew ? 1 : 0;
  }

  async hget(key: string, field: string): Promise<string | null> {
    const hash = this.hashStore.get(key);
    return hash?.get(field) || null;
  }

  async hmget(key: string, ...fields: string[]): Promise<(string | null)[]> {
    const hash = this.hashStore.get(key);
    return fields.map(field => hash?.get(field) || null);
  }

  async hincrby(key: string, field: string, increment: number): Promise<number> {
    if (!this.hashStore.has(key)) {
      this.hashStore.set(key, new Map());
    }
    const hash = this.hashStore.get(key)!;
    const current = parseInt(hash.get(field) || '0');
    const newValue = current + increment;
    hash.set(field, newValue.toString());
    return newValue;
  }

  async hgetall(key: string): Promise<Record<string, string>> {
    const hash = this.hashStore.get(key);
    if (!hash) return {};
    const result: Record<string, string> = {};
    for (const [field, value] of hash.entries()) {
      result[field] = value;
    }
    return result;
  }

  // Set operations
  private setStore = new Map<string, Set<string>>();

  async sadd(key: string, ...members: string[]): Promise<number> {
    if (!this.setStore.has(key)) {
      this.setStore.set(key, new Set());
    }
    const set = this.setStore.get(key)!;
    let added = 0;
    for (const member of members) {
      if (!set.has(member)) {
        set.add(member);
        added++;
      }
    }
    return added;
  }

  async scard(key: string): Promise<number> {
    const set = this.setStore.get(key);
    return set?.size || 0;
  }

  async smembers(key: string): Promise<string[]> {
    const set = this.setStore.get(key);
    return set ? Array.from(set) : [];
  }

  async setnx(key: string, value: string): Promise<number> {
    if (this.store.has(key)) {
      return 0;
    }
    await this.set(key, value);
    return 1;
  }

  // Sorted set operations
  private zsetStore = new Map<string, Map<string, number>>();

  async zadd(key: string, score: number, member: string): Promise<number> {
    if (!this.zsetStore.has(key)) {
      this.zsetStore.set(key, new Map());
    }
    const zset = this.zsetStore.get(key)!;
    const isNew = !zset.has(member);
    zset.set(member, score);
    return isNew ? 1 : 0;
  }

  async zrangebyscore(key: string, min: number, max: number): Promise<string[]> {
    const zset = this.zsetStore.get(key);
    if (!zset) return [];
    
    return Array.from(zset.entries())
      .filter(([, score]) => score >= min && score <= max)
      .sort(([, a], [, b]) => a - b)
      .map(([member]) => member);
  }

  async zremrangebyscore(key: string, min: number, max: number): Promise<number> {
    const zset = this.zsetStore.get(key);
    if (!zset) return 0;
    
    let removed = 0;
    for (const [member, score] of zset.entries()) {
      if (score >= min && score <= max) {
        zset.delete(member);
        removed++;
      }
    }
    return removed;
  }

  async zcard(key: string): Promise<number> {
    const zset = this.zsetStore.get(key);
    return zset?.size || 0;
  }

  // Pipeline support
  pipeline() {
    const commands: Array<() => Promise<any>> = [];
    
    const pipeline = {
      setex: (key: string, seconds: number, value: string) => {
        commands.push(() => this.setex(key, seconds, value));
        return pipeline;
      },
      set: (key: string, value: string) => {
        commands.push(() => this.set(key, value));
        return pipeline;
      },
      del: (...keys: string[]) => {
        commands.push(async () => {
          let count = 0;
          for (const key of keys) {
            count += await this.del(key);
          }
          return count;
        });
        return pipeline;
      },
      expire: (key: string, seconds: number) => {
        commands.push(() => this.expire(key, seconds));
        return pipeline;
      },
      hset: (key: string, field: string, value: string) => {
        commands.push(() => this.hset(key, field, value));
        return pipeline;
      },
      hincrby: (key: string, field: string, increment: number) => {
        commands.push(() => this.hincrby(key, field, increment));
        return pipeline;
      },
      sadd: (key: string, ...members: string[]) => {
        commands.push(() => this.sadd(key, ...members));
        return pipeline;
      },
      zadd: (key: string, score: number, member: string) => {
        commands.push(() => this.zadd(key, score, member));
        return pipeline;
      },
      zremrangebyscore: (key: string, min: number, max: number) => {
        commands.push(() => this.zremrangebyscore(key, min, max));
        return pipeline;
      },
      exec: async () => {
        const results = [];
        for (const command of commands) {
          try {
            const result = await command();
            results.push([null, result]);
          } catch (error) {
            results.push([error, null]);
          }
        }
        return results;
      }
    };
    
    return pipeline;
  }
}

const globalForRedis = globalThis as unknown as {
  redis: SimpleRedis | undefined;
};

export const redis = globalForRedis.redis ?? new SimpleRedis();

if (process.env.NODE_ENV !== 'production') {
  globalForRedis.redis = redis;
}

export default redis;