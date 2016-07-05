/*
 * @formatter:off
 * Li Song Mechlab - A 'mech building tool for PGI's MechWarrior: Online.
 * Copyright (C) 2013  Li Song
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
//@formatter:on
package org.lisoft.lsml.model.export;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Scanner;
import java.util.TreeMap;

import org.lisoft.lsml.command.CmdAddItem;
import org.lisoft.lsml.command.CmdAddModule;
import org.lisoft.lsml.command.CmdSetArmour;
import org.lisoft.lsml.command.CmdSetArmourType;
import org.lisoft.lsml.command.CmdSetGuidanceType;
import org.lisoft.lsml.command.CmdSetHeatSinkType;
import org.lisoft.lsml.command.CmdSetOmniPod;
import org.lisoft.lsml.command.CmdSetStructureType;
import org.lisoft.lsml.command.CmdToggleItem;
import org.lisoft.lsml.model.chassi.ArmourSide;
import org.lisoft.lsml.model.chassi.Chassis;
import org.lisoft.lsml.model.chassi.ChassisClass;
import org.lisoft.lsml.model.chassi.Location;
import org.lisoft.lsml.model.chassi.OmniPod;
import org.lisoft.lsml.model.datacache.ChassisDB;
import org.lisoft.lsml.model.datacache.ItemDB;
import org.lisoft.lsml.model.datacache.OmniPodDB;
import org.lisoft.lsml.model.datacache.PilotModuleDB;
import org.lisoft.lsml.model.datacache.UpgradeDB;
import org.lisoft.lsml.model.item.Internal;
import org.lisoft.lsml.model.item.Item;
import org.lisoft.lsml.model.item.PilotModule;
import org.lisoft.lsml.model.loadout.DefaultLoadoutFactory;
import org.lisoft.lsml.model.loadout.Loadout;
import org.lisoft.lsml.model.loadout.LoadoutBuilder;
import org.lisoft.lsml.model.loadout.LoadoutBuilder.ErrorReportingCallback;
import org.lisoft.lsml.model.loadout.LoadoutOmniMech;
import org.lisoft.lsml.model.loadout.LoadoutStandard;
import org.lisoft.lsml.model.loadout.component.ConfiguredComponent;
import org.lisoft.lsml.model.loadout.component.ConfiguredComponentOmniMech;
import org.lisoft.lsml.model.upgrades.ArmourUpgrade;
import org.lisoft.lsml.model.upgrades.GuidanceUpgrade;
import org.lisoft.lsml.model.upgrades.HeatSinkUpgrade;
import org.lisoft.lsml.model.upgrades.StructureUpgrade;
import org.lisoft.lsml.util.DecodingException;
import org.lisoft.lsml.util.EncodingException;
import org.lisoft.lsml.util.Huffman1;
import org.lisoft.lsml.util.Huffman2;

/**
 * The Third version of {@link LoadoutCoder} for LSML.
 *
 * @author Li Song
 */
public class LoadoutCoderV3 implements LoadoutCoder {
    private static final int HEADER_MAGIC = 0xAC + 2;

    /**
     * Will process the stock builds and generate statistics and dump it to a file.
     *
     * @param arg
     *            Not used
     * @throws Exception
     *             if something went awry.
     */
    public static void main(String[] arg) throws Exception {
        // generateAllLoadouts();
        // generateStatsFromStdin();
        // generateStatsFromStock();
    }

    @SuppressWarnings("unused")
    private static void generateAllLoadouts() throws Exception {
        final List<Chassis> chassii = new ArrayList<>(ChassisDB.lookup(ChassisClass.LIGHT));
        chassii.addAll(ChassisDB.lookup(ChassisClass.MEDIUM));
        chassii.addAll(ChassisDB.lookup(ChassisClass.HEAVY));
        chassii.addAll(ChassisDB.lookup(ChassisClass.ASSAULT));
        final Base64LoadoutCoder coder = new Base64LoadoutCoder(null);
        for (final Chassis chassis : chassii) {
            final Loadout loadout = DefaultLoadoutFactory.instance.produceStock(chassis);
            System.out.println("[" + chassis.getName() + "]=" + coder.encodeLSML(loadout));
        }
    }

    @SuppressWarnings("unused")
    private static void generateStatsFromStdin() throws Exception {
        try (final Scanner sc = new Scanner(System.in, "ASCII");) {

            final int numLoadouts = Integer.parseInt(sc.nextLine());

            final Map<Integer, Integer> freqs = new TreeMap<>();
            String line = sc.nextLine();
            do {
                final String[] s = line.split(" ");
                final int id = Integer.parseInt(s[0]);
                final int freq = Integer.parseInt(s[1]);
                freqs.put(id, freq);
                line = sc.nextLine();
            } while (!line.contains("q"));

            // Make sure all items are in the statistics even if they have a very low probability
            for (final Item item : ItemDB.lookup(Item.class)) {
                final int id = item.getMwoId();
                if (!freqs.containsKey(id)) {
                    freqs.put(id, 1);
                }
            }

            freqs.put(-1, numLoadouts * 9); // 9 separators per loadout
            freqs.put(UpgradeDB.IS_STD_ARMOUR.getMwoId(), numLoadouts * 7 / 10); // Standard armour
            freqs.put(UpgradeDB.IS_FF_ARMOUR.getMwoId(), numLoadouts * 3 / 10); // Ferro Fibrous Armour
            freqs.put(UpgradeDB.IS_STD_STRUCTURE.getMwoId(), numLoadouts * 3 / 10); // Standard structure
            freqs.put(UpgradeDB.IS_ES_STRUCTURE.getMwoId(), numLoadouts * 7 / 10); // Endo-Steel
            freqs.put(UpgradeDB.IS_SHS.getMwoId(), numLoadouts * 1 / 20); // SHS
            freqs.put(UpgradeDB.IS_DHS.getMwoId(), numLoadouts * 19 / 20); // DHS
            freqs.put(UpgradeDB.STD_GUIDANCE.getMwoId(), numLoadouts * 7 / 10); // No Artemis
            freqs.put(UpgradeDB.ARTEMIS_IV.getMwoId(), numLoadouts * 3 / 10); // Artemis IV

            try (final FileOutputStream fos = new FileOutputStream("resources/resources/coderstats_v2.bin");
                    final ObjectOutputStream out = new ObjectOutputStream(fos);) {
                out.writeObject(freqs);
            }
        }
    }

    @SuppressWarnings("unused")
    private static void generateStatsFromStock() throws Exception {
        final Map<Integer, Integer> freqs = new HashMap<>();

        final List<Chassis> chassii = new ArrayList<>(ChassisDB.lookup(ChassisClass.LIGHT));
        chassii.addAll(ChassisDB.lookup(ChassisClass.MEDIUM));
        chassii.addAll(ChassisDB.lookup(ChassisClass.HEAVY));
        chassii.addAll(ChassisDB.lookup(ChassisClass.ASSAULT));
        final List<Integer> idStats = new ArrayList<>();

        // Process items from all stock loadouts
        for (final Chassis chassis : chassii) {
            final Loadout loadout = DefaultLoadoutFactory.instance.produceStock(chassis);

            for (final ConfiguredComponent component : loadout.getComponents()) {
                for (final Item item : component.getItemsEquipped()) {
                    Integer f = freqs.get(item.getMwoId());
                    f = f == null ? 1 : f + 1;
                    freqs.put(item.getMwoId(), f);
                }
            }
        }

        // Add all item ids to the stats list
        for (final Item item : ItemDB.lookup(Item.class)) {
            idStats.add(item.getMwoId());
        }

        // Process omnipods with equal probability
        for (final OmniPod omniPod : OmniPodDB.all()) {
            // Constant frequency of 5, every omnipod appears at most once in the stocks.
            // But this is not representative.
            freqs.put(omniPod.getMwoId(), 5);
            idStats.add(omniPod.getMwoId());
        }

        // Process Pilot modules with equal probability
        for (final PilotModule module : PilotModuleDB.lookup(PilotModule.class)) {
            // Constant frequency of 5, every omnipod appears at most once in the stocks.
            // But this is not representative.
            freqs.put(module.getMwoId(), 3);
            idStats.add(module.getMwoId());
        }

        // Add all unused IDs in the used ranges to the frequency map with frequency 1.
        Collections.sort(idStats);
        int start = -1;
        int last = 0;
        for (final int i : idStats) {
            if (start == -1) {
                start = 1000 * (i / 1000);
            }
            else if (last + 1000 < i) {
                final int end = 1000 * (last / 1000) + 1000;
                for (int id = start; id < end; ++id) {
                    final Integer f = freqs.get(id);
                    if (f == null) {
                        freqs.put(id, 1);
                    }
                }
                System.out.println("Added range: [" + start + ", " + end + "],");
                start = 1000 * (i / 1000);
            }
            last = i;
        }

        // Some manual tweaks

        // 1) Swap DHS and SHS probability for IS mechs.
        {
            final Integer shs = freqs.get(ItemDB.SHS.getMwoId());
            final Integer dhs = freqs.get(ItemDB.DHS.getMwoId());
            freqs.put(ItemDB.SHS.getMwoId(), dhs);
            freqs.put(ItemDB.DHS.getMwoId(), shs);
        }

        // 2) The separators need to be accounted for.
        freqs.put(-1, chassii.size() * 8);

        for (final Entry<Integer, Integer> entry : freqs.entrySet()) {
            String name;
            try {
                final Item item = ItemDB.lookup(entry.getKey());
                name = item.getName();
            }
            catch (final Throwable t) {
                try {
                    final OmniPod omniPod = OmniPodDB.lookup(entry.getKey());
                    name = "omnipod for " + omniPod.getChassisSeries();
                }
                catch (final Throwable t1) {
                    try {
                        final PilotModule module = PilotModuleDB.lookup(entry.getKey());
                        name = module.getName();
                    }
                    catch (final Throwable t2) {
                        name = "reserved id";
                    }
                }
            }

            System.out.println(entry.getKey() + " : " + entry.getValue() + " // " + name);
        }

        try (final FileOutputStream fos = new FileOutputStream("resources/resources/coderstats_v3.bin");
                final ObjectOutputStream out = new ObjectOutputStream(fos);) {
            out.writeObject(freqs);
        }
    }

    private final Huffman2<Integer> huff;

    private final ErrorReportingCallback errorReportingCallback;

    public LoadoutCoderV3(ErrorReportingCallback aErrorReportingCallback) {
        errorReportingCallback = aErrorReportingCallback;
        try (InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("coderstats_v3.bin");
                ObjectInputStream in = new ObjectInputStream(is);) {

            @SuppressWarnings("unchecked")
            final Map<Integer, Integer> freqs = (Map<Integer, Integer>) in.readObject();
            huff = new Huffman2<>(freqs, null);
        }
        catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean canDecode(byte[] aBitStream) {
        final ByteArrayInputStream buffer = new ByteArrayInputStream(aBitStream);
        return buffer.read() == HEADER_MAGIC;
    }

    @Override
    public Loadout decode(final byte[] aBitStream) throws DecodingException {
        final ByteArrayInputStream buffer = new ByteArrayInputStream(aBitStream);

        // Read header
        if (buffer.read() != HEADER_MAGIC) {
            throw new DecodingException("Wrong format!"); // Wrong format
        }

        final LoadoutBuilder builder = new LoadoutBuilder();
        final Loadout loadout = readChassisLoadout(buffer);
        final boolean isOmniMech = loadout instanceof LoadoutOmniMech;

        readArmourValues(buffer, loadout, builder);
        if (isOmniMech) {
            readActuatorState(buffer.read(), loadout, builder);
        }

        // Items are encoded as a list of integers which record the item ID. Components are separated by -1.
        // The order is the same as for armour: RA, RT, RL, HD, CT, LT, LL, LA
        {
            final byte[] rest = new byte[buffer.available()];
            try {
                buffer.read(rest);
            }
            catch (final IOException e) {
                throw new DecodingException(e);
            }
            final List<Integer> ids = huff.decode(rest);
            if (!isOmniMech) {
                final LoadoutStandard loadoutStandard = (LoadoutStandard) loadout;
                builder.push(
                        new CmdSetArmourType(null, loadoutStandard, (ArmourUpgrade) UpgradeDB.lookup(ids.remove(0))));
                builder.push(new CmdSetStructureType(null, loadoutStandard,
                        (StructureUpgrade) UpgradeDB.lookup(ids.remove(0))));
                builder.push(new CmdSetHeatSinkType(null, loadoutStandard,
                        (HeatSinkUpgrade) UpgradeDB.lookup(ids.remove(0))));
            }
            builder.push(new CmdSetGuidanceType(null, loadout, (GuidanceUpgrade) UpgradeDB.lookup(ids.remove(0))));

            for (final Location location : Location.right2Left()) {
                if (isOmniMech && location != Location.CenterTorso) {
                    final LoadoutOmniMech omniMech = (LoadoutOmniMech) loadout;
                    final OmniPod omniPod = OmniPodDB.lookup(ids.remove(0));
                    builder.push(new CmdSetOmniPod(null, omniMech, omniMech.getComponent(location), omniPod));
                }

                Integer v;
                while (!ids.isEmpty() && -1 != (v = ids.remove(0))) {
                    builder.push(new CmdAddItem(null, loadout, loadout.getComponent(location), ItemDB.lookup(v)));
                }
            }

            while (!ids.isEmpty()) {
                builder.push(new CmdAddModule(null, loadout, PilotModuleDB.lookup(ids.remove(0).intValue())));
            }
        }

        builder.apply();
        builder.reportErrors(Optional.of(loadout), errorReportingCallback);
        return loadout;
    }

    /**
     * Encodes the given {@link Loadout} as a bit stream.
     * <p>
     * Bit stream format v3:
     *
     * <pre>
     * <h1>Header:</h1>
     * Stream Offset(bytes)                Comment
     *    0  +---------------------------+
     *       | MAGIC_NUMBER (8bits)      | Must be equal to HEADER_MAGIC.
     *    1  +---------------------------+
     *       | MWO Chassis ID (16bits)   | The MWO ID of the chassis in Big-Endian (Motorola) order (NOTE: x86 is Little-Endian).
     *    3  +---------------------------+
     *       | RA Armour (8bits)         |
     *    4  +---------------------------+
     *       | RT Front Armour (8bits)   |
     *    5  +---------------------------+
     *       | RT Back Armour (8bits)    |
     *    6  +---------------------------+
     *       | RL Armour (8bits)         |
     *    7  +---------------------------+
     *       | HD Armour (8bits)         |
     *    8  +---------------------------+
     *       | CT Front Armour (8bits)   |
     *    9  +---------------------------+
     *       | CT Back Armour (8bits)    |
     *    10 +---------------------------+
     *       | LT Front Armour (8bits)   |
     *    11 +---------------------------+
     *       | LT Back Armour (8bits)    |
     *    12 +---------------------------+
     *       | LL Armour (8bits)         |
     *    13 +---------------------------+
     *       | LA Armour (8bits)         |
     *       +---------------------------+
     *
     * If chassis is an OmniMech:
     *    14 +---------------------------+
     *       | Actuator State (8bits)    | An 8bit map of actuator states. The upper 4 bits are reserved and must be 0.
     *       | Format: 0000XYZW          | Where X and Y is Right LAA and HA respectively, and Z and W is Left LAA and HA respectively.
     *       |                           | The bits are set if the actuators are present and false if the are removed or not available.
     *    15 +---------------------------+
     *       | Huffman coded data        | See further down.
     *   EOS +---------------------------+
     *
     * Else if chassis is a Standard BattleMech:
     *    14 +---------------------------+
     *       | Huffman coded data        | See further down.
     *   EOS +---------------------------+
     *
     * <h1>Huffman coded data</h1>
     * The Huffman coded data is a list (or array if you want) of MWO item IDs and OmniPod IDs that have been encoded to a bit stream.
     * The list contents differs between OmniMechs and standard BattleMechs. The exact Huffman algorithm is defined by
     * {@link Huffman1} and the probability table is given in https://gist.github.com/LiSong-Mechlab/d1af79527270e862cf83c032e64f8083.
     *
     * <h2>OmniMechs Format</h2>
     *  List:
     *    {
     *       (Guidance upgrade ID),
     *       (RA OmniPod ID), {RA equipment IDs}, -1,
     *       (RT OmniPod ID), {RT equipment IDs}, -1,
     *       (RL OmniPod ID), {TL equipment IDs}, -1,
     *       (HD OmniPod ID), {HD equipment IDs}, -1,
     *       {CT equipment IDs}, -1,
     *       (LT OmniPod ID), {LT equipment IDs}, -1,
     *       (LL OmniPod ID), {LL equipment IDs}, -1,
     *       (LT OmniPod ID), {LT equipment IDs}, -1,
     *       {Pilot Modules}
     *    }
     *
     * <h2>Standard BattleMech Format</h2>
     *  List:
     *    {
     *       (Armour upgrade ID),
     *       (Structure upgrade ID),
     *       (Heat sink upgrade ID),
     *       (Guidance upgrade ID),
     *       {RA equipment IDs}, -1,
     *       {RT equipment IDs}, -1,
     *       {TL equipment IDs}, -1,
     *       {HD equipment IDs}, -1,
     *       {CT equipment IDs}, -1,
     *       {LT equipment IDs}, -1,
     *       {LL equipment IDs}, -1,
     *       {LT equipment IDs}, -1,
     *       {Pilot Modules}
     *    }
     *
     * The complete bit stream will be encoded in Base64 and used as link.
     * </pre>
     */
    @Override
    public byte[] encode(final Loadout aLoadout) throws EncodingException {
        final boolean isOmniMech = aLoadout instanceof LoadoutOmniMech;

        final ByteArrayOutputStream buffer = new ByteArrayOutputStream(100);
        buffer.write(HEADER_MAGIC); // 8 bits for version number

        writeChassis(buffer, aLoadout);
        writeArmourValues(buffer, aLoadout);

        if (isOmniMech) {
            writeActuatorState(buffer, aLoadout);
        }

        final List<Integer> ids = new ArrayList<>();

        if (!isOmniMech) {
            ids.add(aLoadout.getUpgrades().getArmour().getMwoId());
            ids.add(aLoadout.getUpgrades().getStructure().getMwoId());
            ids.add(aLoadout.getUpgrades().getHeatSink().getMwoId());
        }
        ids.add(aLoadout.getUpgrades().getGuidance().getMwoId());

        for (final Location location : Location.right2Left()) {
            final ConfiguredComponent component = aLoadout.getComponent(location);
            if (isOmniMech && location != Location.CenterTorso) {
                ids.add(((ConfiguredComponentOmniMech) component).getOmniPod().getMwoId());
            }

            for (final Item item : component.getItemsEquipped()) {
                if (!(item instanceof Internal)) {
                    ids.add(item.getMwoId());
                }
            }
            ids.add(-1);
        }

        final Collection<PilotModule> modules = aLoadout.getModules();
        for (final PilotModule module : modules) {
            ids.add(module.getMwoId());
        }

        // Encode the list with huffman
        try {
            buffer.write(huff.encode(ids));
            return buffer.toByteArray();
        }
        catch (final IOException e) {
            throw new EncodingException(e);
        }
    }

    private void readActuatorState(int aActuatorState, Loadout aLoadout, LoadoutBuilder aBuilder) {
        final boolean RLAA = (aActuatorState & 1 << 3) != 0;
        final boolean RHA = (aActuatorState & 1 << 2) != 0;
        final boolean LLAA = (aActuatorState & 1 << 1) != 0;
        final boolean LHA = (aActuatorState & 1 << 0) != 0;

        final LoadoutOmniMech omniMech = (LoadoutOmniMech) aLoadout;
        aBuilder.push(new CmdToggleItem(null, omniMech, omniMech.getComponent(Location.LeftArm), ItemDB.LAA, LLAA));
        aBuilder.push(new CmdToggleItem(null, omniMech, omniMech.getComponent(Location.LeftArm), ItemDB.HA, LHA));
        aBuilder.push(new CmdToggleItem(null, omniMech, omniMech.getComponent(Location.RightArm), ItemDB.LAA, RLAA));
        aBuilder.push(new CmdToggleItem(null, omniMech, omniMech.getComponent(Location.RightArm), ItemDB.HA, RHA));
    }

    private void readArmourValues(ByteArrayInputStream aBuffer, Loadout aLoadout, LoadoutBuilder aBuilder) {

        // Armour values next, RA, RT, RL, HD, CT, LT, LL, LA
        // 1 byte per armour value (2 for RT,CT,LT front first)
        for (final Location location : Location.right2Left()) {
            final ConfiguredComponent component = aLoadout.getComponent(location);
            for (final ArmourSide side : ArmourSide.allSides(component.getInternalComponent())) {
                aBuilder.push(new CmdSetArmour(null, aLoadout, component, side, aBuffer.read(), true));
            }
        }
    }

    private Loadout readChassisLoadout(ByteArrayInputStream aBuffer) {

        // 16 bits contain chassis ID (Big endian, respecting RFC 1700)
        final short chassisId = (short) ((aBuffer.read() & 0xFF) << 8 | aBuffer.read() & 0xFF);
        final Chassis chassis = ChassisDB.lookup(chassisId);

        return DefaultLoadoutFactory.instance.produceEmpty(chassis);
    }

    private void writeActuatorState(ByteArrayOutputStream aBuffer, Loadout aLoadout) {
        final LoadoutOmniMech omniMech = (LoadoutOmniMech) aLoadout;
        int actuatorState = 0; // 8 bits for actuator toggle states.
        // All actuator states are encoded even if they don't exist on the equipped omnipod. Actuators that don't exist
        // are
        // encoded as false/0.
        actuatorState = actuatorState << 1
                | (omniMech.getComponent(Location.RightArm).getToggleState(ItemDB.LAA) ? 1 : 0);
        actuatorState = actuatorState << 1
                | (omniMech.getComponent(Location.RightArm).getToggleState(ItemDB.HA) ? 1 : 0);
        actuatorState = actuatorState << 1
                | (omniMech.getComponent(Location.LeftArm).getToggleState(ItemDB.LAA) ? 1 : 0);
        actuatorState = actuatorState << 1
                | (omniMech.getComponent(Location.LeftArm).getToggleState(ItemDB.HA) ? 1 : 0);
        aBuffer.write((byte) actuatorState);
    }

    private void writeArmourValues(ByteArrayOutputStream aBuffer, Loadout aLoadout) {
        for (final Location location : Location.right2Left()) {
            final ConfiguredComponent component = aLoadout.getComponent(location);
            for (final ArmourSide side : ArmourSide.allSides(component.getInternalComponent())) {
                aBuffer.write((byte) component.getArmour(side));
            }
        }
    }

    private void writeChassis(ByteArrayOutputStream aBuffer, Loadout aLoadout) {
        // 16 bits (BigEndian, respecting RFC 1700) contains chassis ID.
        final short chassiId = (short) aLoadout.getChassis().getMwoId();
        if (chassiId != aLoadout.getChassis().getMwoId()) {
            throw new RuntimeException("Chassi ID was larger than 16 bits!");
        }
        aBuffer.write((chassiId & 0xFF00) >> 8);
        aBuffer.write(chassiId & 0xFF);
    }
}
