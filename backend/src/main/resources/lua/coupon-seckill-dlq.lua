-- KEYS[1]=DLQ stream, KEYS[2]=dedup index. ARGV: originalMessageId, retryCount, failureCode,
-- failureSummary, firstFailedAt, deadLetteredAt, consumer, orderId, couponId, userId, ttlSeconds.
local streamType = redis.call('TYPE', KEYS[1])['ok']
local indexType = redis.call('TYPE', KEYS[2])['ok']
if (streamType ~= 'none' and streamType ~= 'stream') or (indexType ~= 'none' and indexType ~= 'string') then return {err='REDIS_STRUCTURE_INVALID'} end
if #ARGV ~= 11 or ARGV[1] == '' or tonumber(ARGV[2]) == nil or tonumber(ARGV[11]) == nil then return {err='STREAM_MESSAGE_INVALID'} end
local existing = redis.call('GET', KEYS[2])
if existing then return existing end
local messageId = redis.call('XADD', KEYS[1], '*', 'originalMessageId', ARGV[1], 'retryCount', ARGV[2], 'failureCode', ARGV[3], 'failureSummary', ARGV[4], 'firstFailedAt', ARGV[5], 'deadLetteredAt', ARGV[6], 'consumer', ARGV[7], 'orderId', ARGV[8], 'couponId', ARGV[9], 'userId', ARGV[10])
redis.call('SET', KEYS[2], messageId, 'EX', tonumber(ARGV[11]))
return messageId
