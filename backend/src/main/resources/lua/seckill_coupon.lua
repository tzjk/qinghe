-- KEYS: stock(String), ordered-users(Set), activity-meta(Hash), order-stream(Stream), order-status(Hash)
-- ARGV: userId, couponId, nowMillis, orderId, statusTtlSeconds, optional testRunId
-- Return: 0 accepted, 1 stock exhausted, 2 duplicate order, 3 activity unavailable, 4 invalid state.
if #KEYS ~= 5 or (#ARGV ~= 5 and #ARGV ~= 6) then return 4 end
if not tonumber(ARGV[1]) or not tonumber(ARGV[2]) or not tonumber(ARGV[3]) or not tonumber(ARGV[4])
        or not tonumber(ARGV[5]) or tonumber(ARGV[5]) <= 0 then return 4 end
local function kind(key) return redis.call('TYPE', key)['ok'] end
if kind(KEYS[1]) ~= 'string' or (kind(KEYS[2]) ~= 'none' and kind(KEYS[2]) ~= 'set')
        or kind(KEYS[3]) ~= 'hash' or (kind(KEYS[4]) ~= 'none' and kind(KEYS[4]) ~= 'stream')
        or (kind(KEYS[5]) ~= 'none' and kind(KEYS[5]) ~= 'hash') then return 4 end
local meta = redis.call('HMGET', KEYS[3], 'couponId', 'status', 'startAt', 'endAt')
if not meta[1] or meta[1] ~= ARGV[2] or meta[2] ~= 'ENABLED' or not tonumber(meta[3]) or not tonumber(meta[4]) then return 4 end
if tonumber(ARGV[3]) < tonumber(meta[3]) or tonumber(ARGV[3]) > tonumber(meta[4]) then return 3 end
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then return 2 end
local stock = tonumber(redis.call('GET', KEYS[1]))
if not stock then return 4 end
if stock <= 0 then return 1 end
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
redis.call('HSET', KEYS[5], 'orderId', ARGV[4], 'userId', ARGV[1], 'couponId', ARGV[2], 'status', 'ACCEPTED')
redis.call('EXPIRE', KEYS[5], tonumber(ARGV[5]))
local fields = {'orderId', ARGV[4], 'userId', ARGV[1], 'couponId', ARGV[2]}
if #ARGV == 6 and ARGV[6] ~= '' then table.insert(fields, 'testRunId'); table.insert(fields, ARGV[6]) end
local queued = redis.pcall('XADD', KEYS[4], '*', unpack(fields))
if type(queued) == 'table' and queued['err'] then
    redis.call('SREM', KEYS[2], ARGV[1])
    redis.call('INCR', KEYS[1])
    redis.call('DEL', KEYS[5])
    return 4
end
if #ARGV == 6 and ARGV[6] ~= '' then redis.call('HSET', KEYS[5], 'testRunId', ARGV[6], 'streamMessageId', queued) end
return 0
