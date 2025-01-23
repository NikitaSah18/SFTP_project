package test;

import org.testng.annotations.*;
import static org.testng.Assert.*;
import app.SftpClientStudent;

public class SftpClientTest {

    private SftpClientStudent client;

    @BeforeClass
    public void setUp() {
        client = new SftpClientStudent();
    }

    @Test(description = "Проверка успешного подключения к серверу")
    public void testConnectToSftpServer() {
        client.connectToSftpServer();
        assertNotNull(client.getSession(), "Не удалось подключиться к серверу");
    }

    @Test(description = "Получение списка доменов и IP-адресов")
    public void testGetDomainIpPairs() {
        client.connectToSftpServer();
        client.viewDomainIpPairs();
        assertTrue(client.getDomainIpPairs().size() > 0, "Не удалось получить список доменов и IP-адресов");
    }

    @Test(description = "Получение IP-адреса по домену")
    public void testGetIpByDomain() {
        client.connectToSftpServer();
        String ip = client.getIpByDomain("first.domain");
        assertEquals(ip, "192.168.0.1", "IP не совпадает с ожидаемым");
    }

    @Test(description = "Получение доменного имени по IP-адресу")
    public void testGetDomainByIp() {
        client.connectToSftpServer();
        String domain = client.getDomainByIp("192.168.0.1");
        assertEquals(domain, "first.domain", "Домен не совпадает с ожидаемым");
    }

    @Test(description = "Добавление новой пары домен-IP")
    public void testAddDomainIpPair() {
        client.connectToSftpServer();
        client.addDomainIpPair("new.domain", "192.168.0.4");
        String ip = client.getIpByDomain("new.domain");
        assertEquals(ip, "192.168.0.4", "Новая пара домен-IP не добавлена");
    }

    @Test(description = "Удаление пары домен-IP")
    public void testDeleteDomainIpPair() {
        client.connectToSftpServer();
        client.deleteDomainIpPair("new.domain");
        String ip = client.getIpByDomain("new.domain");
        assertNull(ip, "Пара домен-IP не была удалена");
    }

    @Test(expectedExceptions = IllegalArgumentException.class, description = "Тест: Невалидный IP-адрес")
    public void testInvalidIp() {
        client.addDomainIpPair("invalid.domain", "999.999.999.999");
    }

    @Test(description = "Запрос IP для несуществующего домена")
    public void testInvalidDomain() {
        String ip = client.getIpByDomain("nonexistent.domain");
        assertNull(ip, "Запрос IP для несуществующего домена не должен вернуть результат");
    }


    @AfterClass
    public void tearDown() {
        client.disconnectFromSftp();
    }
}
