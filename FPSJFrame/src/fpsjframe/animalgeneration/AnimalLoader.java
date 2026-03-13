package fpsjframe.animalgeneration;

import java.io.*;
import java.util.*;

/**
 * Parses an animal .txt definition file into an AnimalNode tree.
 *
 * Usage:
 *   AnimalNode root = AnimalLoader.load("animals/goat.txt");
 *
 * File format:
 *   bone: <id> <parent> <abs|rel> <x> <y> <z> <abs|rel> thickness <t> [phantom] [flags...]
 *   Lines starting with # are comments.
 */
public class AnimalLoader {

    public static AnimalNode load(String filepath) throws IOException {
        Map<String, AnimalNode> nodes = new LinkedHashMap<>();
        Map<String, String>     parents = new LinkedHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filepath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("name:")) continue;

                if (line.startsWith("bone:")) {
                    parseBone(line, nodes, parents);
                }
            }
        }

        // Wire up parent-child relationships
        AnimalNode root = null;
        for (Map.Entry<String, String> entry : parents.entrySet()) {
            String childId  = entry.getKey();
            String parentId = entry.getValue();
            AnimalNode child = nodes.get(childId);
            if (parentId.equals("none")) {
                root = child;
            } else {
                AnimalNode parent = nodes.get(parentId);
                if (parent == null) throw new IOException("Unknown parent bone: " + parentId);
                parent.addChild(child);
            }
        }

        if (root == null) throw new IOException("No root bone found (parent=none)");
        return root;
    }

    private static void parseBone(String line,
                                   Map<String, AnimalNode> nodes,
                                   Map<String, String> parents) throws IOException {
        // Remove "bone:" prefix and tokenize
        String[] tok = line.substring(5).trim().split("\\s+");
        // Format: id parent abs|rel x y z abs|rel thickness t [phantom] [flags...]
        if (tok.length < 8) throw new IOException("Malformed bone line: " + line);

        String id       = tok[0];
        String parentId = tok[1];
        boolean posAbs  = tok[2].equalsIgnoreCase("abs");
        float x         = Float.parseFloat(tok[3]);
        float y         = Float.parseFloat(tok[4]);
        float z         = Float.parseFloat(tok[5]);
        boolean thkAbs  = tok[6].equalsIgnoreCase("abs");
        // tok[7] = thickness value (no keyword)
        float thickness = Float.parseFloat(tok[7]);

        boolean phantom = false;
        List<BoneId.Flag> flags = new ArrayList<>();

        for (int i = 8; i < tok.length; i++) {
            switch (tok[i].toLowerCase()) {
                case "phantom" -> phantom = true;
                case "front"   -> flags.add(BoneId.Flag.FRONT);
                case "back"    -> flags.add(BoneId.Flag.BACK);
                case "left"    -> flags.add(BoneId.Flag.LEFT);
                case "right"   -> flags.add(BoneId.Flag.RIGHT);
                case "top"     -> flags.add(BoneId.Flag.TOP);
                case "bottom"  -> flags.add(BoneId.Flag.BOTTOM);
            }
        }

        BoneId boneId = new BoneId(id, flags.toArray(new BoneId.Flag[0]));
        AnimalNode.AbsOrRel<Vec3>  pos  = posAbs
            ? AnimalNode.AbsOrRel.abs(new Vec3(x, y, z))
            : AnimalNode.AbsOrRel.rel(new Vec3(x, y, z));
        AnimalNode.AbsOrRel<Float> thk  = thkAbs
            ? AnimalNode.AbsOrRel.abs(thickness)
            : AnimalNode.AbsOrRel.rel(thickness);

        nodes.put(id, new AnimalNode(boneId, phantom, pos, thk));
        parents.put(id, parentId);
    }
}