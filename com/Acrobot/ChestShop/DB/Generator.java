package com.Acrobot.ChestShop.DB;

import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Config.Config;
import com.Acrobot.ChestShop.Config.Property;
import com.Acrobot.ChestShop.Logging.Logging;
import org.bukkit.Material;

import java.io.*;
import java.util.List;

/**
 * @author Acrobot
 */
public class Generator implements Runnable {
    private static final String filePath = Config.getString(Property.STATISTICS_PAGE_PATH);

    private static double generationTime;

    private static String header;
    private static String row;
    private static String footer;

    private static BufferedWriter buf;

    public void run() {
        header = fileToString("header");
        row = fileToString("row");
        footer = fileToString("footer");

        if (row.isEmpty()) System.err.println(ChestShop.chatPrefix + "You lack the necessary HTML files in your plugins/ChestShop/HTML folder!");
        generateStats();
    }

    private static void fileStart() throws IOException {
        FileWriter fw = new FileWriter(filePath);
        fw.write(header);
        fw.close();
    }

    private static void fileEnd() throws IOException {
        FileWriter fw = new FileWriter(filePath, true);
        fw.write(footer.replace("%time", String.valueOf(generationTime)));
        fw.close();
    }

    private static String fileToString(String fileName) {
        try {
            File f = new File(ChestShop.folder + File.separator + "HTML" + File.separator + fileName + ".html");
            FileReader rd = new FileReader(f);
            char[] buf = new char[(int) f.length()];
            rd.read(buf);
            return new String(buf);
        } catch (Exception e) {
            return "";
        }
    }

    private static double generateItemTotal(int itemID, boolean bought, boolean sold) {
        double amount = 0;
        List<Transaction> list;
        if (bought) list = ChestShop.getDB().find(Transaction.class).where().eq("buy", 1).eq("itemID", itemID).findList();
        else if (sold) list = ChestShop.getDB().find(Transaction.class).where().eq("buy", 0).eq("itemID", itemID).findList();
        else list = ChestShop.getDB().find(Transaction.class).where().eq("itemID", itemID).findList();

        for (Transaction t : list) amount += t.getAmount();
        return amount;
    }

    private static double generateTotalBought(int itemID) {
        return generateItemTotal(itemID, true, false);
    }

    private static double generateTotalSold(int itemID) {
        return generateItemTotal(itemID, false, true);
    }

    private static double generateItemTotal(int itemID) {
        return generateItemTotal(itemID, false, false);
    }

    private static float generateAveragePrice(int itemID) {
        float price = 0;
        List<Transaction> prices = ChestShop.getDB().find(Transaction.class).where().eq("itemID", itemID).eq("buy", true).findList();
        for (Transaction t : prices) price += t.getAveragePricePerItem();
        float toReturn = price / prices.size();
        return (!Float.isNaN(toReturn) ? toReturn : 0);
    }

    private static float generateAverageBuyPrice(int itemID) {
        return generateAveragePrice(itemID);
    }

    private static void generateItemStats(int itemID) throws IOException {
        double total = generateItemTotal(itemID);

        if (total == 0) return;

        double bought = generateTotalBought(itemID);
        double sold = generateTotalSold(itemID);

        Material material = Material.getMaterial(itemID);
        String matName = material.name().replace("_", " ").toLowerCase();

        int maxStackSize = material.getMaxStackSize();

        float buyPrice = generateAverageBuyPrice(itemID);

        buf.write(row.replace("%material", matName)
                .replace("%total", String.valueOf(total))
                .replace("%bought", String.valueOf(bought))
                .replace("%sold", String.valueOf(sold))
                .replace("%maxStackSize", String.valueOf(maxStackSize))
                .replace("%pricePerStack", String.valueOf((buyPrice * maxStackSize)))
                .replace("%pricePerItem", String.valueOf(buyPrice)));
    }

    private static void generateStats() {
        try {

            File f = new File(filePath).getParentFile();
            if (!f.exists()) f.mkdir();

            fileStart();

            buf = new BufferedWriter(new FileWriter(filePath, true));
            long genTime = System.currentTimeMillis();
            for (Material m : Material.values()) generateItemStats(m.getId());

            buf.close();

            generationTime = (System.currentTimeMillis() - genTime) / 1000;
            fileEnd();
        } catch (Exception e) {
            Logging.log("Couldn't generate statistics page!");
            e.printStackTrace();
        }
    }
}
