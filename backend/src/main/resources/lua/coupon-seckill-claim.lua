-- KEYS: stock, claimed-users, activity-meta, stream
-- ARGV: userId, couponId, serverTimeMillis
local status = redis.call('HGET', KEYS[3], 'status')
local startAt = redis.call('HGET', KEYS[3], 'startAt')
local endAt = redis.call('HGET', KEYS[3], 'endAt')
if (not status) or (not startAt) or (not endAt) then return 6 end
if status ~= 'ENABLED' then return 5 end
if tonumber(ARGV[3]) < tonumber(startAt) then return 3 end
if tonumber(ARGV[3]) > tonumber(endAt) then return 4 end
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then return 1 end
local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')
if stock <= 0 then return 2 end
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
redis.call('XADD', KEYS[4], '*', 'couponId', ARGV[2], 'userId', ARGV[1], 'createdAt', ARGV[3])
return 0
