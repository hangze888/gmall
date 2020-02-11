package com.atguigu.gmall.auth;

import com.atguigu.core.utils.JwtUtils;
import com.atguigu.core.utils.RsaUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class GmallAuthApplicationTests {


    private static final String pubKeyPath = "H:\\idea-workspace\\rsa\\rsa.pub";

    private static final String priKeyPath = "H:\\idea-workspace\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "asdjfkl$$slkdjfk&&&");
    }

    @Before
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6MiwidXNlck5hbWUiOiJnZCIsImV4cCI6MTU3OTA5NjAwMH0.eydT9tcyLzPa_TU_K6syeVUou9-t6IjULFlE2fkYzHK5YQn4Q324IZXSnn8LzEaH9DTPnPh8isrtaqSCRjYErfM_iIelQhc9wx2Hkm8aLPRgKOJQ743FvbE0dkVKGtyjI0xDbm1VJ59CHlNy_mDW7ycQ6NfxW8Q2HUI-QSduqTEexeWtKDOGChAbpXlY9TNkwxuRdpNpv2f5P7UJKsFBSteZsIZI-7fHtP8dEr0R3UYByPMgz-fohj74kiHwqkHYMjAVbA1O9t6yvKYrYN28ui53VstHV1pIzPjg8-TmkRDyi8aYJFUMF4x7mMPMbLMRRbhay8Lbd_uxndA9zrBmTg";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("userName"));
    }



    @Test
    public void contextLoads() {

    }

}
