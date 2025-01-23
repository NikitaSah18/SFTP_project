import com.jcraft.jsch.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SftpClientStudent {
    private static final String HOST = "192.168.1.100";
    private static final int PORT = 22;
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String REMOTE_FILE_PATH = "/remote/path/to/domains.json";

    private ChannelSftp channelSftp;
    private Session session;

    public static void main(String[] args) {
        SftpClientStudent client = new SftpClientStudent();
        client.connectToSftpServer();
        client.displayMenu();
    }

    public void connectToSftpServer() {
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(USERNAME, HOST, PORT);
            session.setPassword(PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            Channel channel = session.openChannel("sftp");
            channelSftp = (ChannelSftp) channel;
            channelSftp.connect();
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }

    public void displayMenu() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("1. Просмотр всех пар доменов и IP-адресов");
            System.out.println("2. Получить IP-адрес по домену");
            System.out.println("3. Получить доменное имя по IP-адресу");
            System.out.println("4. Добавить новую пару домен-IP");
            System.out.println("5. Удалить пару домен-IP");
            System.out.println("6. Завершить программу");

            int choice = scanner.nextInt();
            scanner.nextLine();
            switch (choice) {
                case 1:
                    viewDomainIpPairs();
                    break;
                case 2:
                    System.out.print("Введите домен: ");
                    String domain = scanner.nextLine();
                    getIpByDomain(domain);
                    break;
                case 3:
                    System.out.print("Введите IP: ");
                    String ip = scanner.nextLine();
                    getDomainByIp(ip);
                    break;
                case 4:
                    System.out.print("Введите новый домен: ");
                    String newDomain = scanner.nextLine();
                    System.out.print("Введите новый IP: ");
                    String newIp = scanner.nextLine();
                    addDomainIpPair(newDomain, newIp);
                    break;
                case 5:
                    System.out.print("Введите домен или IP для удаления: ");
                    String domainOrIp = scanner.nextLine();
                    deleteDomainIpPair(domainOrIp);
                    break;
                case 6:
                    disconnectFromSftp();
                    return;
                default:
                    System.out.println("Неверный выбор. Пожалуйста, выберите снова.");
            }
        }
    }

    public void viewDomainIpPairs() {
        try {
            InputStream inputStream = channelSftp.get(REMOTE_FILE_PATH);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> data = objectMapper.readValue(inputStream, Map.class);
            List<Map<String, String>> domainIpList = (List<Map<String, String>>) data.get("addresses");
            domainIpList.sort(Comparator.comparing(a -> a.get("domain")));

            for (Map<String, String> entry : domainIpList) {
                System.out.println("Домен: " + entry.get("domain") + " | IP: " + entry.get("ip"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getIpByDomain(String domain) {
        try {
            InputStream inputStream = channelSftp.get(REMOTE_FILE_PATH);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> data = objectMapper.readValue(inputStream, Map.class);
            List<Map<String, String>> domainIpList = (List<Map<String, String>>) data.get("addresses");

            for (Map<String, String> entry : domainIpList) {
                if (entry.get("domain").equals(domain)) {
                    System.out.println("IP-адрес: " + entry.get("ip"));
                    return;
                }
            }
            System.out.println("Домен не найден.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getDomainByIp(String ip) {
        try {
            InputStream inputStream = channelSftp.get(REMOTE_FILE_PATH);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> data = objectMapper.readValue(inputStream, Map.class);
            List<Map<String, String>> domainIpList = (List<Map<String, String>>) data.get("addresses");

            for (Map<String, String> entry : domainIpList) {
                if (entry.get("ip").equals(ip)) {
                    System.out.println("Домен: " + entry.get("domain"));
                    return;
                }
            }
            System.out.println("IP-адрес не найден.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addDomainIpPair(String domain, String ip) {
        if (!isValidIp(ip)) {
            System.out.println("Некорректный IP-адрес.");
            return;
        }

        try {
            InputStream inputStream = channelSftp.get(REMOTE_FILE_PATH);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> data = objectMapper.readValue(inputStream, Map.class);
            List<Map<String, String>> domainIpList = (List<Map<String, String>>) data.get("addresses");

            for (Map<String, String> entry : domainIpList) {
                if (entry.get("domain").equals(domain) || entry.get("ip").equals(ip)) {
                    System.out.println("Этот домен или IP уже существует.");
                    return;
                }
            }

            Map<String, String> newPair = new HashMap<>();
            newPair.put("domain", domain);
            newPair.put("ip", ip);
            domainIpList.add(newPair);

            data.put("addresses", domainIpList);

            OutputStream outputStream = channelSftp.put(REMOTE_FILE_PATH);
            objectMapper.writeValue(outputStream, data);
            System.out.println("Пара домен-IP добавлена.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteDomainIpPair(String domainOrIp) {
        try {
            InputStream inputStream = channelSftp.get(REMOTE_FILE_PATH);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> data = objectMapper.readValue(inputStream, Map.class);
            List<Map<String, String>> domainIpList = (List<Map<String, String>>) data.get("addresses");

            domainIpList.removeIf(entry -> entry.get("domain").equals(domainOrIp) || entry.get("ip").equals(domainOrIp));

            data.put("addresses", domainIpList);

            OutputStream outputStream = channelSftp.put(REMOTE_FILE_PATH);
            objectMapper.writeValue(outputStream, data);
            System.out.println("Пара домен-IP удалена.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isValidIp(String ip) {
        String regex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }

    public void disconnectFromSftp() {
        if (channelSftp != null && channelSftp.isConnected()) {
            channelSftp.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
