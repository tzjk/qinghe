package com.qinghe.life;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.qinghe.life.utils.RedisKeys;
import com.qinghe.life.vo.PublicUserSummaryVO;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExploreSocialContractTest {
    @Test void socialKeysUseRuntimeNamespaceAndNoPiiArgument() { RedisKeys.configureNamespace("qh:test:"); assertTrue(RedisKeys.signIn(7L, "202607").startsWith("qh:test:sign:7:")); assertTrue(RedisKeys.followings(7L).equals("qh:test:followings:7")); assertTrue(RedisKeys.exploreLikers(9L).equals("qh:test:explore:likers:v2:9")); }
    @Test void publicUserSummaryHasNoPrivateFields() { Set<String> fields = new HashSet<String>(); for (Field field : PublicUserSummaryVO.class.getDeclaredFields()) fields.add(field.getName().toLowerCase()); for (String forbidden : Arrays.asList("phone", "realname", "studentno", "password", "token", "dorm", "address", "birthday")) assertFalse(fields.contains(forbidden)); }
}
