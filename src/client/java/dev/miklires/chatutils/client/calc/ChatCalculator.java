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

public final class ChatCalculator {

    private static final MathContext PRECISION = new MathContext(12, RoundingMode.HALF_UP);

    private ChatCalculator() {
    }

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
        } catch (Calculator.CalculatorException e) {
            ChatDelivery.sendSystem(Component.translatable("chatutils.calc.error", e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
        return true;
    }

    public static String evaluateInline(String expression) {
        try {
            return format(Calculator.evaluate(expression));
        } catch (Calculator.CalculatorException e) {
            return "?";
        }
    }

    private static Component answer(String expression, String result) {
        return Component.literal(expression + " = ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(result).withStyle(style -> style
                        .withColor(ChatFormatting.WHITE)
                        .withClickEvent(new ClickEvent.CopyToClipboard(result))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.translatable("chatutils.calc.copy")))));
    }

    private static String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1.0e15) {
            return String.valueOf((long) value);
        }

        BigDecimal rounded = BigDecimal.valueOf(value).round(PRECISION).stripTrailingZeros();
        return rounded.toPlainString();
    }
}
