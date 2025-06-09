import dev.itsvic.parceltracker.api.AnPostDeliveryService
import dev.itsvic.parceltracker.api.MagyarPostaDeliveryService
import dev.itsvic.parceltracker.api.PacketaDeliveryService
import dev.itsvic.parceltracker.api.PolishPostDeliveryService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatValidationTest {
  @Test
  fun polishPost_PocztexFormatReturnsTrue() {
    assertTrue(PolishPostDeliveryService.acceptsFormat("PX1234567890"))
  }

  @Test
  fun polishPost_EMSFormatReturnsTrue() {
    assertTrue(PolishPostDeliveryService.acceptsFormat("EX123456789PL"))
  }

  @Test
  fun polishPost_NonsenseReturnsFalse() {
    assertFalse(PolishPostDeliveryService.acceptsFormat("fsjkdkjjksd"))
  }

  @Test
  fun hungarianPost_EMSFormatReturnsTrue() {
    assertTrue(MagyarPostaDeliveryService.acceptsFormat("EX123456789PL"))
  }

  @Test
  fun hungarianPost_GovFormatReturnsTrue() {
    assertTrue(MagyarPostaDeliveryService.acceptsFormat("RL12345678901234"))
  }

  @Test
  fun hungarianPost_WeirdFormatReturnsTrue() {
    assertTrue(MagyarPostaDeliveryService.acceptsFormat("JJH12AAAAAPL12345678"))
  }

  @Test
  fun hungarianPost_ParcelLockerFormatReturnsTrue() {
    assertTrue(MagyarPostaDeliveryService.acceptsFormat("PNTM1234567890123456789012"))
  }

  @Test
  fun hungarianPost_NonsenseReturnsFalse() {
    assertFalse(MagyarPostaDeliveryService.acceptsFormat("fsjkdkjjksd"))
  }

  @Test
  fun packeta_CanonicalFormat_ReturnsTrue() {
    assertTrue(PacketaDeliveryService.acceptsFormat("Z 123 4567 890"))
  }

  @Test
  fun packeta_CanonicalFormatNoSpaces_ReturnsTrue() {
    assertTrue(PacketaDeliveryService.acceptsFormat("Z1234567890"))
  }

  @Test
  fun anPostFormatReturnsTrue() {
    assertTrue(AnPostDeliveryService.acceptsFormat("Ay123456789Ie"))
  }

  @Test
  fun anPostFormatReturnsFalse() {
    assertFalse(AnPostDeliveryService.acceptsFormat("AY12345678HA"))
  }
}
