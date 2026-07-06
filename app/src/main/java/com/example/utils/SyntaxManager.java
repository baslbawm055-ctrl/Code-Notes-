package com.example.utils;

import android.content.Context;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import com.amrdeveloper.codeview.CodeView;
import com.example.R;
import java.util.regex.Pattern;

public class SyntaxManager {

    private static final Pattern PATTERN_KEYWORDS = Pattern.compile(
            "\\b(public|private|protected|class|interface|extends|implements|new|return|if|else|while|for|do|break|continue|switch|case|default|try|catch|finally|throw|throws|static|final|void|int|double|float|long|boolean|byte|short|char|null|true|false|this|super|import|package|fun|var|val|let|const|function|def|in|is|as|type|typealias|object|companion|inline|operator|infix|out|reified|suspend|override|abstract|open|enum|data|sealed|yield|await|async|await|with|from)\\b");

    private static final Pattern PATTERN_BUILTINS = Pattern.compile(
            "\\b(String|List|Map|Set|Array|Collection|Iterable|Iterator|Int|Double|Float|Long|Boolean|Byte|Short|Char|Unit|Any|Nothing|console|Math|Object|Promise|System|Math|Math|document|window|print|println|require)\\b");

    private static final Pattern PATTERN_STRINGS = Pattern.compile("\"(?:\\\\\"|[^\"])*?\"|'(?:\\\\'|[^'])*?'|`[^`]*?`");
    private static final Pattern PATTERN_COMMENTS = Pattern.compile("//.*|/\\*(?:.|[\\n\\r])*?\\*/|#.*");
    private static final Pattern PATTERN_NUMBERS = Pattern.compile("\\b(\\d*[.]?\\d+)\\b");
    private static final Pattern PATTERN_ANNOTATIONS = Pattern.compile("@\\w+");

    public static void applySyntax(Context context, CodeView codeView) {
        codeView.resetSyntaxPatternList();

        android.content.SharedPreferences prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        float fontSize = prefs.getFloat("font_size", 16f);
        codeView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, fontSize);

        boolean isDarkMode = (context.getResources().getConfiguration().uiMode & 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;

        int colorKeyword = isDarkMode ? Color.parseColor("#CC7832") : Color.parseColor("#000080"); 
        int colorBuiltin = isDarkMode ? Color.parseColor("#9876AA") : Color.parseColor("#660E7A"); 
        int colorString = isDarkMode ? Color.parseColor("#6A8759") : Color.parseColor("#008000"); 
        int colorComment = isDarkMode ? Color.parseColor("#808080") : Color.parseColor("#808080"); 
        int colorNumber = isDarkMode ? Color.parseColor("#6897BB") : Color.parseColor("#0000FF"); 
        int colorAnnotation = isDarkMode ? Color.parseColor("#BBB529") : Color.parseColor("#808000"); 

        // Applying syntax patterns (Order matters!)
        codeView.addSyntaxPattern(PATTERN_NUMBERS, colorNumber);
        codeView.addSyntaxPattern(PATTERN_BUILTINS, colorBuiltin);
        codeView.addSyntaxPattern(PATTERN_KEYWORDS, colorKeyword);
        codeView.addSyntaxPattern(PATTERN_ANNOTATIONS, colorAnnotation);
        codeView.addSyntaxPattern(PATTERN_STRINGS, colorString);
        codeView.addSyntaxPattern(PATTERN_COMMENTS, colorComment);

        codeView.setEnableLineNumber(true);
        codeView.setLineNumberTextColor(Color.GRAY);
        codeView.setLineNumberTextSize(codeView.getTextSize() * 0.85f);
        
        codeView.setEnableHighlightCurrentLine(true);
        codeView.setHighlightCurrentLineColor(isDarkMode ? Color.parseColor("#22FFFFFF") : Color.parseColor("#11000000"));
        
        codeView.reHighlightSyntax();
    }
}
