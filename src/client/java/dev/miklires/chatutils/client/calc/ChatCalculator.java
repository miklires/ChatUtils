package dev.miklires.chatutils.client.calc;

import dev.miklires.chatutils.client.chat.ChatDelivery;
import dev.miklires.chatutils.client.config.ChatUtilsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Evaluates arithmetic typed into the chat.
 *
 * <p>Two entry points, because the two things you want from a calculator are different. A message
 * that <em>is</em> an expression — {@code =17*3} — is answered privately and never sent; nobody else
 * wanted to see your sums. An expression <em>inside</em> a message — {@code that's [=17*3] blocks} —
 * is replaced before sending, because there the number is the point of the sentence.
 */
public final class ChatCalculator {

    /** Beyond this many digits a double is lying to you anyway. */
    private static final MathContext PRECISION = new MathContext(12, RoundingMode.HALF_UP);

    private ChatCalculator() {
    }

    /**
     * Handles a message the player is sending.
     *
     * @return {@code true} when the message was an expression and has been answered, so it must not
     *         be sent to the server
     */
    public static boolean handleOutgoing(String message) {
        ChatUtilsConfig config = ChatUtilsConfig.get();
        String prefix = config.calculatorPrefix;
        if (!config.calculatorEnabled || prefix == null || prefix.isEmpty() || !message.startsWith(prefix)) {
            return false;
        }

        String expression = message.substring(prefix.length());
        try {
            String result = format(Calculator.evaluate(expression));
            ChatDelivery.sendSystem(answer(expression.trim(), result));
        } catch (Calculator.CalculatorException failure) {
            ChatDelivery.sendSystem(Component.translatable("chatutils.calc.error", failure.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
        return true;
    }

    /** Evaluates an expression embedded in an outgoing message, for {@code MacroExpander}. */
    public static String evaluateInline(String expression) {
        try {
            return format(Calculator.evaluate(expression));
        } catch (Calculator.CalculatorException failure) {
            return "?";
        }
    }

    /** The answer line, clickable to copy so the number can leave the game. */
    private static Component answer(String expression, String result) {
        return Component.literal(expression + " = ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(result).withStyle(style -> style
                        .withColor(ChatFormatting.WHITE)
                        .withClickEvent(new ClickEvent.CopyToClipboard(result))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.translatable("chatutils.calc.copy")))));
    }

    /**
     * Formats the result the way a person would write it: whole numbers without a decimal point,
     * everything else trimmed of the noise that binary floating point leaves on the end.
     */
    private static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1.0e15) {
            return String.valueOf((long) value);
        }

        BigDecimal rounded = BigDecimal.valueOf(value).round(PRECISION).stripTrailingZeros();
        return rounded.toPlainString();
    }
}
