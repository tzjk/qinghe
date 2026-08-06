-- KEYS: stock(String), ordered-users(Set), order-status(Hash)
-- ARGV: userId, couponId
-- Only a terminal FAILED reservation can be compensated once.
if #KEYS ~= 3 or #ARGV ~= 2 then return 0 end
if redis.call('TYPE', KEYS[1])['ok'] ~= 'string' or redis.call('TYPE', KEYS[2])['ok'] ~= 'set'
        or redis.call('TYPE', KEYS[3])['ok'] ~= 'hash' then return 0 end
local status = redis.call('HGET', KEYS[3], 'status')
if status == 'SUCCESS' or status == 'FAILED' then return 0 end
if redis.call('HGET', KEYS[3], 'userId') ~= ARGV[1] or redis.call('HGET', KEYS[3], 'couponId') ~= ARGV[2] then return 0 end
redis.call('HSET', KEYS[3], 'status', 'FAILED')
redis.call('SREM', KEYS[2], ARGV[1])
redis.call('INCR', KEYS[1])
return 1
