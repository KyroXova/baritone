package baritone.launch.mixins;

import java.lang.reflect.Method;
import net.minecraft.item.Item;
import org.junit.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.junit.Assert.assertEquals;

public class MixinItemStackTest {
    @Test
    public void damageGetterMaySetDamageWithoutRecursing() throws Exception {
        Method changed = MixinItemStack.class.getDeclaredMethod("onItemDamageSet", int.class, CallbackInfo.class);
        changed.setAccessible(true);
        class DynamicDamageStack extends MixinItemStack {
            final Item item = new Item();
            int calls;

            @Override public Item getItem() { return item; }
            @Override public int getItemDamage() {
                if (++calls > 2) throw new AssertionError("Damage getter recursively reentered");
                try {
                    changed.invoke(this, 7, new CallbackInfo("setItemDamage", false));
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
                return 7;
            }
        }
        DynamicDamageStack stack = new DynamicDamageStack();
        assertEquals(stack.item.hashCode() + 7, stack.getBaritoneHash());
        assertEquals(1, stack.calls);
        changed.invoke(stack, 7, new CallbackInfo("setItemDamage", false));
        assertEquals(stack.item.hashCode() + 7, stack.getBaritoneHash());
        assertEquals(2, stack.calls);
    }
}
