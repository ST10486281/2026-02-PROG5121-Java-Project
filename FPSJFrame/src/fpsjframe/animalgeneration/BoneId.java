package fpsjframe.animalgeneration;

import java.util.Arrays;
import java.util.List;

/**
 * Identifier for a bone in the skeleton.
 * Equivalent to BoneId + BoneFlag in AnimalNode.hs
 */
public class BoneId {

    /** Built-in spatial flags for bones - equivalent to BoneFlag in AnimalNode.hs */
    public enum Flag {
        FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM
    }

    public final String     name;
    public final List<Flag> flags;

    public BoneId(String name, Flag... flags) {
        this.name  = name;
        this.flags = Arrays.asList(flags);
    }

    public boolean hasFlag(Flag f)  { return flags.contains(f); }
    public boolean hasName(String n) { return name.equals(n); }

    /** Mirror LEFT<->RIGHT flags (for ReflZ symmetry) */
    public BoneId mirrorZ() {
        Flag[] mirrored = flags.stream().map(f -> {
            if (f == Flag.LEFT)  return Flag.RIGHT;
            if (f == Flag.RIGHT) return Flag.LEFT;
            return f;
        }).toArray(Flag[]::new);
        return new BoneId(name, mirrored);
    }

    /** Mirror FRONT<->BACK flags (for ReflX symmetry) */
    public BoneId mirrorX() {
        Flag[] mirrored = flags.stream().map(f -> {
            if (f == Flag.FRONT) return Flag.BACK;
            if (f == Flag.BACK)  return Flag.FRONT;
            return f;
        }).toArray(Flag[]::new);
        return new BoneId(name, mirrored);
    }

    @Override
    public String toString() { return "BoneId(" + name + ", " + flags + ")"; }
}