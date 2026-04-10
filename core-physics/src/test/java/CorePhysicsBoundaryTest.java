import static org.junit.jupiter.api.Assertions.assertEquals;

import com.gravity.remake.corephysics.api.GravityDirection;
import com.gravity.remake.corephysics.api.InputFrame;
import org.junit.jupiter.api.Test;

class CorePhysicsBoundaryTest {
  @Test
  void inputFrameStoresDeterministicTick() {
    InputFrame frame = new InputFrame(1.0, true, GravityDirection.LEFT, 42L);

    assertEquals(42L, frame.tick());
  }
}
