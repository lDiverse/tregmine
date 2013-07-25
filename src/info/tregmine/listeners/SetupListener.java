package info.tregmine.listeners;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;

import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import info.tregmine.Tregmine;
import info.tregmine.api.TregminePlayer;
import info.tregmine.api.PlayerReport;
import info.tregmine.database.ConnectionPool;
import info.tregmine.database.DBPlayerDAO;
import info.tregmine.database.DBPlayerReportDAO;

public class SetupListener implements Listener
{
    public static class KickTask implements Runnable
    {
        private TregminePlayer player;
        private String welcomeDateStr;

        public KickTask(TregminePlayer player, String welcomeDateStr)
        {
            this.player = player;
            this.welcomeDateStr = welcomeDateStr;
        }

        @Override
        public void run()
        {
            if (!player.isOnline()) {
                return;
            }

            player.kickPlayer("Younger than 13. Welcome back on " +
                    welcomeDateStr + "!");
        }
    }

    private final static SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private final static long AGE_LIMIT = 13L * 60L * 60L * 24L * 365L * 1000L;

    private Tregmine plugin;

    public SetupListener(Tregmine instance)
    {
        this.plugin = instance;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        TregminePlayer player = plugin.getPlayer(event.getPlayer());
        if (player.getChatState() != TregminePlayer.ChatState.SETUP) {
            return;
        }

        Tregmine.LOGGER.info("[SETUP] " + player.getChatName() + " is a new player!");

        player.sendMessage(ChatColor.BLUE + "Welcome to Tregmine!");
        player.sendMessage(ChatColor.BLUE + "Use the chat to enter your date of " +
                "birth to continue. For example: 2001-12-24");
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event)
    {
        TregminePlayer player = plugin.getPlayer(event.getPlayer());
        if (player.getChatState() != TregminePlayer.ChatState.SETUP) {
            return;
        }

        event.setCancelled(true);

        String text = event.getMessage();
        player.sendMessage(text);

        if (player.isChild()) {
            return;
        }

        Tregmine.LOGGER.info("[SETUP] <" + player.getChatName() + "> " + text);

        String[] dateSplit = text.split("-");
        if (dateSplit.length != 3) {
            player.sendMessage(ChatColor.RED + "Use the following format: " +
                               "YYYY-MM-DD. For example: 2001-12-24");
            return;
        }

        Date enteredDate;
        try {
            enteredDate = FORMAT.parse(text);
        } catch (ParseException e) {
            player.sendMessage(ChatColor.RED + "Use the following format: " +
                               "YYYY-MM-DD. For example: 2001-12-24");
            return;
        }

        Date currentDate = new Date();

        long age = currentDate.getTime() - enteredDate.getTime();

        Connection conn = null;
        try {
            conn = ConnectionPool.getConnection();

            long diff = AGE_LIMIT - age;
            Server server = plugin.getServer();
            if (diff < 0) {
                player.sendMessage(ChatColor.BLUE + "You have now joined Tregmine " +
                        "and can talk with other players! Say Hi! :)");
                player.setChatState(TregminePlayer.ChatState.CHAT);
                player.setSetup(true);

                DBPlayerDAO playerDAO = new DBPlayerDAO(conn);
                playerDAO.updatePlayerPermissions(player);

                Tregmine.LOGGER.info("[SETUP] " + player.getChatName() +
                        " joined the server. Born on: " + text);

                server.broadcastMessage(ChatColor.GREEN + "Welcome to Tregmine, " +
                        player.getChatName() + ChatColor.GREEN + "!");
            }
            else {
                player.setChild(true);
                player.setNameColor("child");

                Date welcomeDate = new Date(currentDate.getTime() + diff);
                String welcomeDateStr = FORMAT.format(welcomeDate);

                player.sendMessage(ChatColor.BLUE + "Unfortunately Tregmine has an " +
                        "age limit of 13 years and older. Because of that you have " +
                        "been automatically banned until you are old enough to play here.");
                player.sendMessage(ChatColor.BLUE + "In one minute you will be " +
                        "automatically kicked.");
                player.sendMessage(ChatColor.BLUE + "Welcome back on " + welcomeDateStr + "!");

                String message = "Banned until " + welcomeDateStr + " due to age limit.";

                PlayerReport report = new PlayerReport();
                report.setSubjectId(player.getId());
                report.setIssuerId(0);
                report.setAction(PlayerReport.Action.BAN);
                report.setMessage(message);
                report.setValidUntil(welcomeDate);

                DBPlayerReportDAO reportDAO = new DBPlayerReportDAO(conn);
                reportDAO.insertReport(report);

                BukkitScheduler scheduler = server.getScheduler();
                scheduler.scheduleSyncDelayedTask(plugin,
                        new KickTask(player, welcomeDateStr), 20 * 60);

                Tregmine.LOGGER.info("[SETUP] " + player.getChatName() +
                        " is being banned from server. Born on: " + text);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }
}