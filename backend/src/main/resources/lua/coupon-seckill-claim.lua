-- KEYS: stock(String), claimed-users(Set), activity-meta(Hash), main-stream(Stream), reservation(Hash)
-- ARGV: userId, couponId, serverTimeMillis, orderId, reservationTtlSeconds
-- Codes: 0 accepted, 1 duplicate, 2 stock, 3 not-started, 4 ended, 5 disabled,
-- 6 not-ready, 7 invalid arguments, 8 Redis structure invalid, 9 enqueue/rollback failure.
local function keyType(key) return redis.call('TYPE', key)['ok'] end
if #KEYS ~= 5 or #ARGV ~= 5 then return 7 end
if not tonumber(ARGV[1]) or not tonumber(ARGV[2]) or not tonumber(ARGV[3]) or not tonumber(ARGV[5])
        or ARGV[4] == '' or string.len(ARGV[4]) > 96 or tonumber(ARGV[5]) <= 0 then return 7 end
local stockType, usersType, metaType, streamType, reservationType = keyType(KEYS[1]), keyType(KEYS[2]), keyType(KEYS[3]), keyType(KEYS[4]), keyType(KEYS[5])
if stockType ~= 'string' or (usersType ~= 'none' and usersType ~= 'set') or metaType ~= 'hash'
        or (streamType ~= 'none' and streamType ~= 'stream') or (reservationType ~= 'none' and reservationType ~= 'hash') then return 8 end
local status, startAt, endAt, metaCouponId = redis.call('HMGET', KEYS[3], 'status', 'startAt', 'endAt', 'couponId')[1], redis.call('HGET', KEYS[3], 'startAt'), redis.call('HGET', KEYS[3], 'endAt'), redis.call('HGET', KEYS[3], 'couponId')
if not status or not startAt or not endAt or not metaCouponId or metaCouponId ~= ARGV[2] or not tonumber(startAt) or not tonumber(endAt) then return 6 end
if status ~= 'ENABLED' then return 5 end
if tonumber(ARGV[3]) < tonumber(startAt) then return 3 end
if tonumber(ARGV[3]) > tonumber(endAt) then return 4 end
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then return 1 end
local stock = tonumber(redis.call('GET', KEYS[1]))
if not stock then return 8 end
if stock <= 0 then return 2 end

redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
redis.call('HMSET', KEYS[5], 'orderId', ARGV[4], 'userId', ARGV[1], 'couponId', ARGV[2], 'status', 'RESERVED', 'messageStatus', 'PENDING', 'createdAt', ARGV[3])
redis.call('EXPIRE', KEYS[5], tonumber(ARGV[5]))
local queued = redis.pcall('XADD', KEYS[4], '*', 'orderId', ARGV[4], 'couponId', ARGV[2], 'userId', ARGV[1], 'createdAt', ARGV[3])
if type(queued) == 'table' and queued['err'] then
    if redis.call('SREM', KEYS[2], ARGV[1]) == 1 then redis.call('INCR', KEYS[1]) end
    redis.call('DEL', KEYS[5])
    return 9
end
redis.call('HMSET', KEYS[5], 'messageStatus', 'ENQUEUED', 'streamMessageId', queued)
return 0
