package noc.nsync.connect.ssh;

import expect4j.Closure;
import expect4j.Expect4j;
import expect4j.ExpectState;
import expect4j.matches.Match;
import expect4j.matches.RegExpMatch;
import org.apache.oro.text.regex.MalformedPatternException;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

public class SshClient {

    private static final int COMMAND_EXECUTION_SUCCESS_OPCODE = -2;
    private static String ENTER_CHARACTER = "\r";
    private static final int SSH_PORT = 22;
    private List<String> lstCmds = new ArrayList<String>();
    private static String[] linuxPromptRegEx = new String[]{"\\>","#", "~#", "$"};

    private Expect4j expect = null;
    private StringBuilder buffer = new StringBuilder();
    private String userName;
    private String password;
    private String host;

    /**
     *
     * @param host remote machine IP or Hostname
     * @param userName user at the remote machine
     * @param password user's password
     */
    public SshClient(String host, String userName, String password) {
        this.host = host;
        this.userName = userName;
        this.password = password;
    }
    /**
     *
     * @param cmdsToExecute command to execute
     */
    public String execute(List<String> cmdsToExecute) {
        this.lstCmds = cmdsToExecute;

        Closure closure = new Closure() {
            public void run(ExpectState expectState) throws Exception {
                buffer.append(expectState.getBuffer());
            }
        };
        List<Match> lstPattern =  new ArrayList<Match>();
        for (String regexElement : linuxPromptRegEx) {
            try {
                Match mat = new RegExpMatch(regexElement, closure);
                lstPattern.add(mat);
            } catch (MalformedPatternException e) {
                e.printStackTrace();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        try {
            expect = SSH();
            boolean isSuccess = true;
            for(String strCmd : lstCmds) {
                isSuccess = isSuccess(lstPattern,strCmd);
                if (!isSuccess) {
                    isSuccess = isSuccess(lstPattern,strCmd);
                }
            }

            checkResult(expect.expect(lstPattern));
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeConnection();
        }
        return buffer.toString();
    }

    private boolean isSuccess(List<Match> objPattern,String strCommandPattern) {
        try {
            boolean isFailed = checkResult(expect.expect(objPattern));

            if (!isFailed) {
                expect.send(strCommandPattern);
                expect.send(ENTER_CHARACTER);
                return true;
            }
            return false;
        } catch (MalformedPatternException ex) {
            ex.printStackTrace();
            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private Expect4j SSH() throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(userName, host, SSH_PORT);
        if (password != null) {
            session.setPassword(password);
        }

        Properties properties = new Properties();
        properties.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
        properties.put("StrictHostKeyChecking", "no");
        properties.put("UseDNS", "no");
        session.setConfig(properties);
        session.connect(30_000);
        ChannelShell channel = (ChannelShell) session.openChannel("shell");
        Expect4j expect = new Expect4j(channel.getInputStream(), channel.getOutputStream());
        channel.connect();
        return expect;
    }

    private boolean checkResult(int intRetVal) {
        if (intRetVal == COMMAND_EXECUTION_SUCCESS_OPCODE) {
            return true;
        }
        return false;
    }

    private void closeConnection() {
        if (expect!=null) {
            expect.close();
        }
    }

    public static void main(String[] args) {
        SshClient ssh = new SshClient("host", "user", "pwd");
        List<String> commandsToExecute = new ArrayList<String>();
        commandsToExecute.add("ls");
        commandsToExecute.add("pwd");
        commandsToExecute.add("mkdir testdir");
        String outputLog = ssh.execute(commandsToExecute);
        System.out.println(outputLog);
    }
}