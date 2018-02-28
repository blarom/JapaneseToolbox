package com.japanesetoolboxapp;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.japanesetoolboxapp.utiities.*;

public class ConvertFragment extends Fragment {

    // Fragment Lifecycle Functions
        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            // Retain this fragment (used to save user inputs on activity creation/destruction)
            setRetainInstance(true);

            // Define that this fragment is related to fragment_conjugator.xml
            View fragmentView = inflater.inflate(R.layout.fragment_convert, container, false);


            return fragmentView;
        }
        @Override public void onStart() {
        super.onStart();

        String outputFromInputQueryFragment = getArguments().getString("input_to_fragment");
        getConversion(outputFromInputQueryFragment);
    }

    // Fragment Modules
        public void getConversion(final String inputQuery) {

            // Gets the output of the InputQueryFragment and makes it available to the current fragment

            TextView Conversion = (TextView)getActivity().findViewById(R.id.conversion);
            TextView ConversionLatin = (TextView)getActivity().findViewById(R.id.conversion_latin);
            TextView ConversionHiragana = (TextView)getActivity().findViewById(R.id.conversion_hiragana);
            TextView ConversionKatakana = (TextView)getActivity().findViewById(R.id.conversion_katakana);
            TextView ResultLatin = (TextView)getActivity().findViewById(R.id.Result_latin);
            TextView ResultHiragana = (TextView)getActivity().findViewById(R.id.Result_hiragana);
            TextView ResultKatakana = (TextView)getActivity().findViewById(R.id.Result_katakana);

            if (Kana_to_Romaji_to_Kana(inputQuery).get(0).equals("no_input")) {
                Conversion.setText("Please enter a word.");
                ConversionLatin.setText("");
                ConversionHiragana.setText("");
                ConversionKatakana.setText("");
                ResultLatin.setText("");
                ResultHiragana.setText("");
                ResultKatakana.setText("");
            }
            else {
                Conversion.setText("Transliteration:");
                ConversionLatin.setText("Latin Script: ");
                ConversionHiragana.setText("Hiragana: ");
                ConversionKatakana.setText("Katakana: ");
                ResultLatin.setText(Kana_to_Romaji_to_Kana(inputQuery).get(0));
                ResultHiragana.setText(Kana_to_Romaji_to_Kana(inputQuery).get(1));
                ResultKatakana.setText(Kana_to_Romaji_to_Kana(inputQuery).get(2));

            }
        }
 		public static List<List<String>> GlobalVerbSpinnerList;
 		public static List<List<String>> GlobalConjugationsSpinnerList;
 		public static int SpinnerState;

	    public static List<String> Kana_to_Romaji_to_Kana(String input_value) {

            List<String> translation = new ArrayList<>();
			String translation_latin = "";
            String translation_hiragana = "";
            String translation_katakana = "";
            translation.add(translation_latin);
            translation.add(translation_hiragana);
            translation.add(translation_katakana);

			String character;
			if (!input_value.equals("")) { character = Character.toString(input_value.charAt(0)); }
            else { return translation; }

            translation_latin = "";
            translation_hiragana = "" ;
            translation_katakana = "";

            String added_string_latin;
            String added_string_hiragana;
            String added_string_katakana;
            String last_added_string_latin = "";
            String last_added_string_hiragana = "";
            String last_added_string_katakana = "";

            String character_next;
            String character_next2;
            String character_last;
            String added_string;
            String added_string_last = "";
            List<String> scriptdetectorOutput;
            List<String> charFinderOutput;

            int final_index = 0;
            if (!input_value.equals("")) {final_index = input_value.length()-1;}
            boolean skip = false;

            for (int i=0; i <= final_index; i++) {
                character_next = "";
                character_next2 = "";
                character_last = "";

                character = Character.toString(input_value.charAt(i));
                if (i <= final_index-1) { character_next  = Character.toString(input_value.charAt(i+1));}
                if (i <= final_index-2) { character_next2 = Character.toString(input_value.charAt(i+2));}
                if (i>0) { character_last = Character.toString(input_value.charAt(i-1));}

                // Detecting what the current character represents
                    scriptdetectorOutput = GetPhonemeBasedOnLetter(i, character, character_next, character_next2, character_last);

                    i = Integer.parseInt(scriptdetectorOutput.get(0));
                    added_string = scriptdetectorOutput.get(1);

                // Getting the current string addition
                    charFinderOutput = GetCharBasedOnPhoneme(i, added_string, input_value, final_index, character, character_next, added_string_last);

                    added_string_last = added_string;

                    i = Integer.parseInt(charFinderOutput.get(0));
                    added_string_latin = charFinderOutput.get(1);
                    added_string_hiragana = charFinderOutput.get(2);
                    added_string_katakana = charFinderOutput.get(3);

                    // Add the string to the translation
                    translation_latin = translation_latin + added_string_latin;
                    translation_hiragana = translation_hiragana + added_string_hiragana;
                    translation_katakana = translation_katakana + added_string_katakana;

            }

            translation.set(0, SharedMethods.SpecialConcatenator(translation_latin));
            translation.set(1, SharedMethods.SpecialConcatenator(translation_hiragana));
            translation.set(2, SharedMethods.SpecialConcatenator(translation_katakana));
			return translation;
		}
        public static String TextType(String input_value) {

            input_value = SharedMethods.SpecialConcatenator(input_value);
            String character;
            String text_type = "";

            if (!input_value.equals("")) {
                for (int i=0; i<input_value.length();i++) {

                    if (text_type.equals("kanji")) { break;}

                    character = Character.toString(input_value.charAt(i));

                    if (character.equals("あ")
                            || character.equals("い")
                            || character.equals("う")
                            || character.equals("え")
                            || character.equals("お")
                            || character.equals("か")
                            || character.equals("き")
                            || character.equals("く")
                            || character.equals("け")
                            || character.equals("こ")
                            || character.equals("が")
                            || character.equals("ぎ")
                            || character.equals("ぐ")
                            || character.equals("げ")
                            || character.equals("ご")
                            || character.equals("さ")
                            || character.equals("す")
                            || character.equals("せ")
                            || character.equals("そ")
                            || character.equals("ざ")
                            || character.equals("ず")
                            || character.equals("ぜ")
                            || character.equals("ぞ")
                            || character.equals("し")
                            || character.equals("じ")
                            || character.equals("た")
                            || character.equals("て")
                            || character.equals("と")
                            || character.equals("だ")
                            || character.equals("で")
                            || character.equals("ど")
                            || character.equals("ち")
                            || character.equals("つ")
                            || character.equals("づ")
                            || character.equals("な")
                            || character.equals("ぬ")
                            || character.equals("ね")
                            || character.equals("の")
                            || character.equals("ん")
                            || character.equals("に")
                            || character.equals("は")
                            || character.equals("ひ")
                            || character.equals("へ")
                            || character.equals("ほ")
                            || character.equals("ば")
                            || character.equals("び")
                            || character.equals("べ")
                            || character.equals("ぼ")
                            || character.equals("ぱ")
                            || character.equals("ぴ")
                            || character.equals("ぺ")
                            || character.equals("ぽ")
                            || character.equals("ふ")
                            || character.equals("ま")
                            || character.equals("み")
                            || character.equals("む")
                            || character.equals("め")
                            || character.equals("も")
                            || character.equals("や")
                            || character.equals("ゆ")
                            || character.equals("よ")
                            || character.equals("ら")
                            || character.equals("り")
                            || character.equals("る")
                            || character.equals("れ")
                            || character.equals("ろ")
                            || character.equals("わ")
                            || character.equals("う")
                            || character.equals("を")
                            || character.equals("ゔ")
                            || character.equals("っ")
                            || character.equals("ゐ")
                            || character.equals("ゑ")
                            || character.equals("ぢ")
                            || character.equals("づ")
                            || character.equals("ぁ")
                            || character.equals("ゃ")
                            || character.equals("ゅ")
                            || character.equals("ぅ")
                            || character.equals("ょ")
                            || character.equals("ぉ")
                            || character.equals("ぇ")
                            || character.equals("ぃ")) {
                        text_type = "hiragana";
                    } else if (character.equals("ア")
                            || character.equals("イ")
                            || character.equals("ウ")
                            || character.equals("エ")
                            || character.equals("オ")
                            || character.equals("カ")
                            || character.equals("キ")
                            || character.equals("ク")
                            || character.equals("ケ")
                            || character.equals("コ")
                            || character.equals("ガ")
                            || character.equals("ギ")
                            || character.equals("グ")
                            || character.equals("ゲ")
                            || character.equals("ゴ")
                            || character.equals("サ")
                            || character.equals("ス")
                            || character.equals("セ")
                            || character.equals("ソ")
                            || character.equals("ザ")
                            || character.equals("ズ")
                            || character.equals("ゼ")
                            || character.equals("ゾ")
                            || character.equals("シ")
                            || character.equals("ジ")
                            || character.equals("タ")
                            || character.equals("テ")
                            || character.equals("ト")
                            || character.equals("ダ")
                            || character.equals("デ")
                            || character.equals("ド")
                            || character.equals("チ")
                            || character.equals("ツ")
                            || character.equals("ヅ")
                            || character.equals("ナ")
                            || character.equals("ヌ")
                            || character.equals("ネ")
                            || character.equals("ノ")
                            || character.equals("ン")
                            || character.equals("ニ")
                            || character.equals("ハ")
                            || character.equals("ヒ")
                            || character.equals("ヘ")
                            || character.equals("ホ")
                            || character.equals("バ")
                            || character.equals("ビ")
                            || character.equals("ベ")
                            || character.equals("ボ")
                            || character.equals("パ")
                            || character.equals("ピ")
                            || character.equals("ペ")
                            || character.equals("ポ")
                            || character.equals("フ")
                            || character.equals("マ")
                            || character.equals("ミ")
                            || character.equals("ム")
                            || character.equals("メ")
                            || character.equals("モ")
                            || character.equals("ヤ")
                            || character.equals("ユ")
                            || character.equals("ヨ")
                            || character.equals("ラ")
                            || character.equals("リ")
                            || character.equals("ル")
                            || character.equals("レ")
                            || character.equals("ロ")
                            || character.equals("ワ")
                            || character.equals("ウ")
                            || character.equals("ヲ")
                            || character.equals("ヴ")
                            || character.equals("ー")
                            || character.equals("ッ")
                            || character.equals("ヰ")
                            || character.equals("ヱ")
                            || character.equals("ァ")
                            || character.equals("ャ")
                            || character.equals("ュ")
                            || character.equals("ゥ")
                            || character.equals("ォ")
                            || character.equals("ョ")
                            || character.equals("ェ")
                            || character.equals("ィ")) {
                        text_type = "katakana";
                    } else if (character.equalsIgnoreCase("a") || character.equalsIgnoreCase("b")
                            || character.equalsIgnoreCase("c") || character.equalsIgnoreCase("d")
                            || character.equalsIgnoreCase("e") || character.equalsIgnoreCase("f")
                            || character.equalsIgnoreCase("g") || character.equalsIgnoreCase("h")
                            || character.equalsIgnoreCase("i") || character.equalsIgnoreCase("j")
                            || character.equalsIgnoreCase("k") || character.equalsIgnoreCase("l")
                            || character.equalsIgnoreCase("m") || character.equalsIgnoreCase("n")
                            || character.equalsIgnoreCase("o") || character.equalsIgnoreCase("p")
                            || character.equalsIgnoreCase("q") || character.equalsIgnoreCase("r")
                            || character.equalsIgnoreCase("s") || character.equalsIgnoreCase("t")
                            || character.equalsIgnoreCase("u") || character.equalsIgnoreCase("v")
                            || character.equalsIgnoreCase("w") || character.equalsIgnoreCase("x")
                            || character.equalsIgnoreCase("y") || character.equalsIgnoreCase("z")) {
                        text_type = "latin";
                    } else if (character.equalsIgnoreCase("0") || character.equalsIgnoreCase("1")
                            || character.equalsIgnoreCase("2") || character.equalsIgnoreCase("3")
                            || character.equalsIgnoreCase("4") || character.equalsIgnoreCase("5")
                            || character.equalsIgnoreCase("6") || character.equalsIgnoreCase("7")
                            || character.equalsIgnoreCase("8") || character.equalsIgnoreCase("9")) {
                        text_type = "number";
                    } else {
                        text_type = "kanji";
                    }
                }
            } else {
                return text_type;
            }

            return text_type;
        }
        public static List<String> GetPhonemeBasedOnLetter(int i, String character, String character_next, String character_next2, String character_last) {

            String added_string = "";
            // Hiragana script detection

                ;// Vowels
                if      (character.equals("あ")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "a_double_vowel"; }
                    else { added_string = "a"; }
                }
                else if (character.equals("い")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "i_double_vowel"; }
                    else if (character_next.equals("ぇ")) { added_string = "ye"; i++; }
                    else { added_string = "i"; }
                }
                else if (character.equals("う")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "u_double_vowel"; }
                    else { added_string = "u"; }
                }
                else if (character.equals("え")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "e_double_vowel"; }
                    else { added_string = "e"; }
                }
                else if (character.equals("お")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "o_double_vowel"; }
                    else { added_string = "o"; }
                }

                // k, g
                else if (character.equals("か")) { added_string = "ka"; }
                else if (character.equals("き")) {
                    if      (character_next.equals("ゃ")) { added_string = "kya"; i++; }
                    else if (character_next.equals("ゅ")) { added_string = "kyu"; i++; }
                    else if (character_next.equals("ょ")) { added_string = "kyo"; i++; }
                    else { added_string = "ki"; }
                }
                else if (character.equals("く")) { added_string = "ku"; }
                else if (character.equals("け")) { added_string = "ke"; }
                else if (character.equals("こ")) { added_string = "ko"; }

                else if (character.equals("が")) { added_string = "ga"; }
                else if (character.equals("ぎ")) {
                    if      (character_next.equals("ゃ")) { added_string = "gya"; i++; }
                    else if (character_next.equals("ゅ")) { added_string = "gyu"; i++; }
                    else if (character_next.equals("ょ")) { added_string = "gyo"; i++; }
                    else { added_string = "gi"; }
                }
                else if (character.equals("ぐ")) { added_string = "gu"; }
                else if (character.equals("げ")) { added_string = "ge"; }
                else if (character.equals("ご")) { added_string = "go"; }

                // s, z, sh, j
                else if (character.equals("さ")) { added_string = "sa"; }
                else if (character.equals("す")) { added_string = "su"; }
                else if (character.equals("せ")) { added_string = "se"; }
                else if (character.equals("そ")) { added_string = "so"; }

                else if (character.equals("ざ")) { added_string = "za"; }
                else if (character.equals("ず")) { added_string = "zu"; }
                else if (character.equals("ぜ")) { added_string = "ze"; }
                else if (character.equals("ぞ")) { added_string = "zo"; }

                else if (character.equals("し")) {
                    if      (character_next.equals("ゃ")) { added_string = "sha"; i++; }
                    else if (character_next.equals("ゅ")) { added_string = "shu"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "she"; i++; }
                    else if (character_next.equals("ょ")) { added_string = "sho"; i++; }
                    else { added_string = "shi"; }
                }

                else if (character.equals("じ")) {
                    if      (character_next.equals("ゃ")) { added_string = "ja"; i++; }
                    else if (character_next.equals("ゅ")) { added_string = "ju"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "je"; i++; }
                    else if (character_next.equals("ょ")) { added_string = "jo"; i++; }
                    else { added_string = "ji"; }
                }

                // t, d, ch, ts, dz
                else if (character.equals("た")) { added_string = "ta"; }
                else if (character.equals("て")) { added_string = "te"; }
                else if (character.equals("と")) { added_string = "to"; }

                else if (character.equals("だ")) { added_string = "da"; }
                else if (character.equals("で")) { added_string = "de"; }
                else if (character.equals("ど")) { added_string = "do"; }

                else if (character.equals("ち")) {
                    if      (character_next.equals("ゃ")) { added_string = "cha"; i++; }
                    else if (character_next.equals("ゅ")) { added_string = "chu"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "che"; i++; }
                    else if (character_next.equals("ょ")) { added_string = "cho"; i++; }
                    else { added_string = "chi"; }
                }

                else if (character.equals("ぢ")) {
                    if      (character_next.equals("ゃ")) {	added_string = "*"; i++; }
                    else if (character_next.equals("ゅ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ょ")) { added_string = "*"; i++; }
                    else { added_string = "di"; }
                }

                else if (character.equals("つ")) {
                    if      (character_next.equals("ぁ")) { added_string = "tsa"; i++; }
                    else if (character_next.equals("ぃ")) { added_string = "tsi"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "tse"; i++; }
                    else if (character_next.equals("ぉ")) { added_string = "tso"; i++; }
                    else { added_string = "tsu"; }
                }

                else if (character.equals("づ")) {
                    if      (character_next.equals("ぁ")) { added_string = "da"; i++; }
                    else if (character_next.equals("ぃ")) { added_string = "di"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "de"; i++; }
                    else if (character_next.equals("ぉ")) { added_string = "do"; i++; }
                    else { added_string = "du"; }
                }

                // n, ny
                else if (character.equals("な")) { added_string = "na"; }
                else if (character.equals("ぬ")) { added_string = "nu"; }
                else if (character.equals("ね")) { added_string = "ne"; }
                else if (character.equals("の")) { added_string = "no"; }
                else if (character.equals("ん")) {
                    if      (character_next.equals("あ")) { added_string = "n'"; }
                    else if (character_next.equals("え")) { added_string = "n'"; }
                    else if (character_next.equals("い")) { added_string = "n'"; }
                    else if (character_next.equals("お")) { added_string = "n'"; }
                    else if (character_next.equals("う")) { added_string = "n'"; }
                    else if (character_next.equals("や")) { added_string = "n'"; }
                    else if (character_next.equals("よ")) { added_string = "n'"; }
                    else if (character_next.equals("ゆ")) { added_string = "n'"; }
                    else { added_string = "n"; }
                }
                else if (character.equals("に")) {
                    if      (character_next.equals("ゃ")) { added_string = "nya"; i++; }
                    else if (character_next.equals("ゅ")) { added_string = "nyu"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "nye"; i++; }
                    else if (character_next.equals("ょ")) { added_string = "nyo"; i++; }
                    else { added_string = "ni"; }
                }

                // h, b, p, hy, by ,py
                else if (character.equals("は")) { added_string = "ha"; }
                else if (character.equals("ひ")) {
                    if      (character_next.equals("ゃ")) { added_string = "hya"; i++; }
                    else if (character_next.equals("ゅ")) { added_string = "hyu"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "hye"; i++; }
                    else if (character_next.equals("ょ")) { added_string = "hyo"; i++; }
                    else { added_string = "hi"; }
                }
                else if (character.equals("へ")) { added_string = "he"; }
                else if (character.equals("ほ")) { added_string = "ho"; }

                else if (character.equals("ば")) { added_string = "ba"; }
                else if (character.equals("び")) {
                    if      (character_next.equals("ゃ")) { added_string = "bya"; i++; }
                    else if (character_next.equals("ゅ")) { added_string = "byu"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "bye"; i++; }
                    else if (character_next.equals("ょ")) { added_string = "byo"; i++; }
                    else { added_string = "bi"; }
                }
                else if (character.equals("べ")) { added_string = "be"; }
                else if (character.equals("ぼ")) { added_string = "bo"; }
                else if (character.equals("ぶ")) { added_string = "bu"; }

                else if (character.equals("ぱ")) { added_string = "pa"; }
                else if (character.equals("ぴ")) {
                    if      (character_next.equals("ゃ")) { added_string = "pya"; i++; }
                    else if (character_next.equals("ゅ")) { added_string = "pyu"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "pye"; i++; }
                    else if (character_next.equals("ょ")) { added_string = "pyo"; i++; }
                    else { added_string = "pi"; }
                }
                else if (character.equals("ぺ")) { added_string = "pe"; }
                else if (character.equals("ぽ")) { added_string = "po"; }
                else if (character.equals("ぷ")) { added_string = "pu"; }

                // f
                else if (character.equals("ふ")) {
                    if      (character_next.equals("ぁ")) { added_string = "fa"; i++; }
                    else if (character_next.equals("ぃ")) { added_string = "fi"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "fe"; i++; }
                    else if (character_next.equals("ぉ")) { added_string = "fo"; i++; }
                    else if (character_next.equals("ゃ")) { added_string = "fya"; i++; }
                    else if (character_next.equals("ゅ")) { added_string = "fyu"; i++; }
                    else if (character_next.equals("ょ")) { added_string = "fyo"; i++; }
                    else { added_string = "fu"; }
                }

                // m, my
                else if (character.equals("ま")) { added_string = "ma"; }
                else if (character.equals("み")) {
                    if      (character_next.equals("ゃ")) { added_string = "mya"; i++; }
                    else if (character_next.equals("ゅ")) { added_string = "myu"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "mye"; i++; }
                    else if (character_next.equals("ょ")) { added_string = "myo"; i++; }
                    else { added_string = "mi"; }
                }
                else if (character.equals("む")) { added_string = "mu"; }
                else if (character.equals("め")) { added_string = "me"; }
                else if (character.equals("も")) { added_string = "mo"; }

                // y
                else if (character.equals("や")) { added_string = "ya"; }
                else if (character.equals("ゆ")) { added_string = "yu"; }
                else if (character.equals("よ")) { added_string = "yo"; }

                // r, ry
                else if (character.equals("ら")) { added_string = "ra"; }
                else if (character.equals("り")) {
                    if      (character_next.equals("ゃ")) { added_string = "rya"; i++; }
                    else if (character_next.equals("ゅ")) { added_string = "ryu"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "rye"; i++; }
                    else if (character_next.equals("ょ")) { added_string = "ryo"; i++; }
                    else { added_string = "ri"; }
                }
                else if (character.equals("る") ) { added_string = "ru"; }
                else if (character.equals("れ")) { added_string = "re"; }
                else if (character.equals("ろ")) { added_string = "ro"; }

                // w, v
                else if (character.equals("わ")) { added_string = "wa"; }
                else if (character.equals("う")) {
                    if      (character_next.equals("ぃ")) { added_string = "wi"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "we"; i++; }
                    else { added_string = "wu"; }
                }
                else if (character.equals("ゐ")) { added_string = "wi"; }
                else if (character.equals("ゑ")) { added_string = "we"; }
                else if (character.equals("を")) { added_string = "wo"; }

                else if (character.equals("ゔ")) {
                    if      (character_next.equals("ぁ")) { added_string = "va"; i++; }
                    else if (character_next.equals("ぃ")) { added_string = "vi"; i++; }
                    else if (character_next.equals("ぇ")) { added_string = "ve"; i++; }
                    else if (character_next.equals("ぉ")) { added_string = "vo"; i++; }
                    else { added_string = "vu"; }
                }

                // Obsolete kanas
                else if (character.equals("ゐ")) { added_string = "Xwi"; }
                else if (character.equals("ゑ")) { added_string = "Xwe"; }

                // Hiragana double consonant
                else if (character.equals("っ")) { added_string = "small_tsu"; }


            // Katakana script detection

                // Vowels
                else if (character.equals("ア")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "a_double_vowel"; }
                    else { added_string = "a"; }
                }
                else if (character.equals("イ")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "i_double_vowel"; }
                    else if (character_next.equals("ェ")) { added_string = "ye"; i++; }
                    else { added_string = "i"; }
                }
                else if (character.equals("ウ")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "u_double_vowel"; }
                    else { added_string = "u"; }
                }
                else if (character.equals("エ")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "e_double_vowel"; }
                    else { added_string = "e"; }
                }
                else if (character.equals("オ")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "o_double_vowel"; }
                    else { added_string = "o"; }
                }

                // k, g
                else if (character.equals("カ")) { added_string = "ka"; }
                else if (character.equals("キ")) {
                    if      (character_next.equals("ャ")) { added_string = "kya"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "kyu"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "kye"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "kyo"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "kyi"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "ki"; }
                }
                else if (character.equals("ク")) { added_string = "ku"; }
                else if (character.equals("ケ")) { added_string = "ke"; }
                else if (character.equals("コ")) { added_string = "ko"; }

                else if (character.equals("ガ")) { added_string = "ga"; }
                else if (character.equals("ギ")) {
                    if      (character_next.equals("ャ")) { added_string = "gya"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "gyu"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "gye"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "gyo"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "gyi"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "gi"; }
                }
                else if (character.equals("グ")) { added_string = "gu"; }
                else if (character.equals("ゲ")) { added_string = "ge"; }
                else if (character.equals("ゴ")) { added_string = "go"; }

                // s, z, sh, j
                else if (character.equals("サ")) { added_string = "sa"; }
                else if (character.equals("ス")) { added_string = "su"; }
                else if (character.equals("セ")) { added_string = "se"; }
                else if (character.equals("ソ")) { added_string = "so"; }

                else if (character.equals("ザ")) { added_string = "za"; }
                else if (character.equals("ズ")) { added_string = "zu"; }
                else if (character.equals("ゼ")) { added_string = "ze"; }
                else if (character.equals("ゾ")) { added_string = "zo"; }

                else if (character.equals("シ")) {
                    if      (character_next.equals("ャ")) { added_string = "sha"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "shu"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "she"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "sho"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "shi"; }
                }

                else if (character.equals("ジ")) {
                    if      (character_next.equals("ャ")) { added_string = "ja"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "ju"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "je"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "jo"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "ji"; }
                }

                // t, d, ch, ts, dz
                else if (character.equals("タ")) { added_string = "ta"; }
                else if (character.equals("テ")) {
                    if (character_next.equals("ィ")) { i++; }
                    else if (character_next.equals("ュ")) {added_string = "tu"; i++; }
                    else {added_string = "te"; }
                }
                else if (character.equals("ト")) { added_string = "to";
                    if (character_next.equals("ャ")) {added_string = "ta"; i++; }
                    else if (character_next.equals("ゥ")) { added_string = "tu"; i++;}
                    else if (character_next.equals("ェ")) {added_string = "te"; i++; }
                    else if (character_next.equals("ィ")) {added_string = "ti"; i++; }
                    else if (character_next.equals("ョ")) {added_string = "to"; i++; }
                }

                else if (character.equals("ダ")) { added_string = "da"; }
                else if (character.equals("デ")) {
                    if      (character_next.equals("ャ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "du"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*";i++; }
                    else if (character_next.equals("ィ")) { added_string = "di"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "de"; }
                }
                else if (character.equals("ド")) { added_string = "do"; }

                else if (character.equals("チ")) {
                    if      (character_next.equals("ャ")) { added_string = "cha"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "chu"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "che"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "cho"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "chi"; }
                }

                else if (character.equals("ヂ")) {
                    if      (character_next.equals("ャ")) { added_string = "dja"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "dju"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "dje"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "djo"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "dji"; }
                }

                else if (character.equals("ツ")) {
                    if      (character_next.equals("ャ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "tse"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "tsa"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "tsi"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "tso"; i++; }
                    else { added_string = "tsu"; }
                }

                else if (character.equals("ヅ")) {
                    if      (character_next.equals("ャ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "dze"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "dza"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "dzi"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "dzo"; i++; }
                    else { added_string = "dzu"; }
                }

                // n, ny
                else if (character.equals("ナ")) { added_string = "na"; }
                else if (character.equals("ヌ")) { added_string = "nu"; }
                else if (character.equals("ネ")) { added_string = "ne"; }
                else if (character.equals("ノ")) { added_string = "no"; }
                else if (character.equals("ン")) {
                    if      (character_next.equals("ア")) { added_string = "n'"; }
                    else if (character_next.equals("エ")) { added_string = "n'"; }
                    else if (character_next.equals("イ")) { added_string = "n'"; }
                    else if (character_next.equals("オ")) { added_string = "n'"; }
                    else if (character_next.equals("ウ")) { added_string = "n'"; }
                    else if (character_next.equals("ヤ")) { added_string = "n'"; }
                    else if (character_next.equals("ヨ")) { added_string = "n'"; }
                    else if (character_next.equals("ユ")) { added_string = "n'"; }
                    else { added_string = "n"; }
                }
                else if (character.equals("ニ")) {
                    if      (character_next.equals("ャ")) { added_string = "nya"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "nyu"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "nye"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "nyo"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "nyi"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "ni"; }
                }

                // h, b, p, hy, by ,py
                else if (character.equals("ハ")) { added_string = "ha"; }
                else if (character.equals("ヒ")) {
                    if      (character_next.equals("ャ")) { added_string = "hya"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "hyu"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "hye"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "hyo"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "hyi"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "hi"; }
                }
                else if (character.equals("ヘ")) { added_string = "he"; }
                else if (character.equals("ホ")) { added_string = "ho"; }

                else if (character.equals("バ")) { added_string = "ba"; }
                else if (character.equals("ビ")) {
                    if      (character_next.equals("ャ")) { added_string = "bya"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "byu"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "bye";  i++; }
                    else if (character_next.equals("ョ")) { added_string = "byo"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "byi"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "bi"; }
                }
                else if (character.equals("ベ")) { added_string = "be"; }
                else if (character.equals("ボ")) { added_string = "bo"; }
                else if (character.equals("ブ")) { added_string = "bu"; }

                else if (character.equals("パ")) { added_string = "pa"; }
                else if (character.equals("ピ")) {
                    if      (character_next.equals("ャ")) { added_string = "pya"; i++; }
                    else if (character_next.equals("ユ")) { added_string = "pyu"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "pye"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "pyo"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "pyi"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "pi"; }
                }
                else if (character.equals("ペ")) { added_string = "pe"; }
                else if (character.equals("ポ")) { added_string = "po"; }
                else if (character.equals("プ")) { added_string = "pu"; }

                // f
                else if (character.equals("フ")) {
                    if      (character_next.equals("ャ")) { added_string = "fya"; i++; }
                    else if (character_next.equals("ユ")) { added_string = "fyu"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "fe"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "fyo"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "fa"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "fi"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "fo"; i++; }
                    else { added_string = "fu"; }
                }

                // m, my
                else if (character.equals("マ")) { added_string = "ma"; }
                else if (character.equals("ミ")) {
                    if      (character_next.equals("ャ")) { added_string = "mya"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "myu"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "mye"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "myo"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "mi"; i++; }
                }
                else if (character.equals("ム")) { added_string = "mu"; }
                else if (character.equals("メ")) { added_string = "me"; }
                else if (character.equals("モ")) { added_string = "mo"; }

                // y
                else if (character.equals("ヤ")) { added_string = "ya"; }
                else if (character.equals("ユ")) { added_string = "yu"; }
                else if (character.equals("ヨ")) { added_string = "yo"; }

                // r, ry
                else if (character.equals("ラ")) { added_string = "ra"; }
                else if (character.equals("リ")) {
                    if      (character_next.equals("ャ")) { added_string = "rya"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "ryu"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "rye"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "ryo"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "ri"; }
                }
                else if (character.equals("ル")) { added_string = "ru"; }
                else if (character.equals("レ")) { added_string = "re"; }
                else if (character.equals("ロ")) { added_string = "ro"; }

                // w, v
                else if (character.equals("ワ")) { added_string = "wa"; }
                else if (character.equals("ウ")) {
                    if      (character_next.equals("ャ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ュ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "we"; i++; }
                    else if (character_next.equals("ョ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "wi"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "wu"; }
                }
                else if (character.equals("ヰ")) { added_string = "wi"; }
                else if (character.equals("ヱ")) { added_string = "we"; }
                else if (character.equals("ヲ")) { added_string = "wo"; }

                else if (character.equals("ヴ")) {
                    if      (character_next.equals("ァ")) { added_string = "va"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "vi"; i++; }
                    else if (character_next.equals("ェ")) { added_string = "ve"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "vo"; i++; }
                    else if (character_next.equals("ァ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ィ")) { added_string = "*"; i++; }
                    else if (character_next.equals("ォ")) { added_string = "*"; i++; }
                    else { added_string = "vu"; }
                }
                else if (character.equals("ヷ")) { added_string = "va"; }
                else if (character.equals("ヸ")) { added_string = "vi"; }
                else if (character.equals("ヹ")) { added_string = "ve"; }
                else if (character.equals("ヺ")) { added_string = "vo"; }

                else if (character.equals("ヰ")) { added_string = "Xwi"; }
                else if (character.equals("ヱ")) { added_string = "Xwe"; }

                // Katakana double consonant
                else if (character.equals("ッ")) { added_string = "small_tsu"; }

                // Katakana repreated vowel
                else if (character.equals("ー")) { added_string = "katakana_repeat_bar"; }

            // Latin script detection
                else if (character.equalsIgnoreCase("a")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "a_double_vowel"; }
                    else { added_string = "a"; }
                }

                else if (character.equalsIgnoreCase("b")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "ba"; i++; }
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "be"; i++; }
                    else if (character_next.equalsIgnoreCase("i")) { added_string = "bi"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "bo"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "bu"; i++; }
                    else if (character_next.equalsIgnoreCase("y")) {
                        if      (character_next2.equalsIgnoreCase("a")) { added_string = "bya"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("e")) { added_string = "bye"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("i")) { added_string = "byi"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("o")) { added_string = "byo"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("u")) { added_string = "byu"; i++; i++; }
                        else { added_string = "*"; }
                    }
                    else if (character_next.equalsIgnoreCase("b")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("c")) {
                    if (character_next.equalsIgnoreCase("h")) {
                        if      (character_next2.equalsIgnoreCase("a")) { added_string = "cha"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("e")) { added_string = "che"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("i")) { added_string = "chi"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("o")) { added_string = "cho"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("u")) { added_string = "chu"; i++; i++; }
                        else { added_string = "*"; }
                    }
                    else if (character_next.equalsIgnoreCase("c")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("d")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "da"; i++; }
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "de"; i++; }
                    else if (character_next.equalsIgnoreCase("i")) { added_string = "di"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "do"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "du"; i++; }
                    else if (character_next.equalsIgnoreCase("d")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("e")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "e_double_vowel"; }
                    else { added_string = "e"; }
                }

                else if (character.equalsIgnoreCase("f")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "fa"; i++; }
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "fe"; i++; }
                    else if (character_next.equalsIgnoreCase("i")) { added_string = "fi"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "fo"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "fu"; i++; }
                    else if (character_next.equalsIgnoreCase("y")) {
                        if      (character_next2.equalsIgnoreCase("a")) { added_string = "fya"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("e")) { added_string = "fye"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("i")) { added_string = "fyi"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("o")) { added_string = "fyo"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("u")) { added_string = "fyu"; i++; i++; }
                        else { added_string = "*"; }
                    }
                    else if (character_next.equalsIgnoreCase("f")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("g")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "ga"; i++; }
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "ge"; i++; }
                    else if (character_next.equalsIgnoreCase("i")) { added_string = "gi"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "go"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "gu"; i++; }
                    else if (character_next.equalsIgnoreCase("y")) {
                        if      (character_next2.equalsIgnoreCase("a")) { added_string = "gya"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("e")) { added_string = "gye"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("i")) { added_string = "gyi"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("o")) { added_string = "gyo"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("u")) { added_string = "gyu"; i++; i++; }
                        else { added_string = "*"; }
                    }
                    else if (character_next.equalsIgnoreCase("g")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("h")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "ha"; i++;}
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "he"; i++; }
                    else if (character_next.equalsIgnoreCase("i")) { added_string = "hi"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "ho"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "hu"; i++; }
                    else if (character_next.equalsIgnoreCase("y")) {
                        if      (character_next2.equalsIgnoreCase("a")) { added_string = "hya"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("e")) { added_string = "hye"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("i")) { added_string = "hyi"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("o")) { added_string = "hyo"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("u")) { added_string = "hyu"; i++; i++; }
                        else { added_string = "*"; }
                    }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("i")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "i_double_vowel"; }
                    else { added_string = "i"; }
                }

                else if (character.equalsIgnoreCase("j")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "ja"; i++;}
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "je"; i++; }
                    else if (character_next.equalsIgnoreCase("i")) { added_string = "ji"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "jo"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "ju"; i++; }
                    else if (character_next.equalsIgnoreCase("y")) {
                        if      (character_next2.equalsIgnoreCase("a")) { added_string = "jya"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("e")) { added_string = "jye"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("i")) { added_string = "jyi"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("o")) { added_string = "jyo"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("u")) { added_string = "jyu"; i++; i++; }
                        else { added_string = "*"; }
                    }
                    else if (character_next.equalsIgnoreCase("j")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("k")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "ka"; i++; }
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "ke"; i++; }
                    else if (character_next.equalsIgnoreCase("i")) { added_string = "ki"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "ko"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "ku"; i++; }
                    else if (character_next.equalsIgnoreCase("y")) {
                        if      (character_next2.equalsIgnoreCase("a")) { added_string = "kya"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("e")) { added_string = "kye"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("i")) { added_string = "kyi"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("o")) { added_string = "kyo"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("u")) { added_string = "kyu"; i++; i++; }
                        else { added_string = "*"; }
                    }
                    else if (character_next.equalsIgnoreCase("k")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("l")) { added_string = "*"; }

                else if (character.equalsIgnoreCase("m")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "ma"; i++; }
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "me"; i++; }
                    else if (character_next.equalsIgnoreCase("i")) { added_string = "mi"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "mo"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "mu"; i++; }
                    else if (character_next.equalsIgnoreCase("y")) {
                        if      (character_next2.equalsIgnoreCase("a")) { added_string = "mya"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("e")) { added_string = "mye"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("i")) { added_string = "myi"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("o")) { added_string = "myo"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("u")) { added_string = "myu"; i++; i++; }
                        else { added_string = "*"; }
                    }
                    else if (character_next.equalsIgnoreCase("m")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("n")) {
                    if      (character_next.equalsIgnoreCase("'")) { added_string = "n'"; i++; }
                    else if (character_next.equalsIgnoreCase("a")) { added_string = "na"; i++; }
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "ne"; i++; }
                    else if (character_next.equalsIgnoreCase("i")) { added_string = "ni"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "no"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "nu"; i++; }
                    else if (character_next.equalsIgnoreCase("y")) {
                        if      (character_next2.equalsIgnoreCase("a")) { added_string = "nya"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("e")) { added_string = "nye"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("i")) { added_string = "nyi"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("o")) { added_string = "nyo"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("u")) { added_string = "nyu"; i++; i++; }
                        else { added_string = "*"; i++; }
                    }
                    else { added_string = "n"; }
                }

                else if (character.equalsIgnoreCase("o")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "o_double_vowel"; }
                    else { added_string = "o"; }
                }

                else if (character.equalsIgnoreCase("p")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "pa"; i++;}
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "pe"; i++; }
                    else if (character_next.equalsIgnoreCase("i")) { added_string = "pi"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "po"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "pu"; i++; }
                    else if (character_next.equalsIgnoreCase("y")) {
                        if      (character_next2.equalsIgnoreCase("a")) { added_string = "pya"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("e")) { added_string = "pye"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("i")) { added_string = "pyi"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("o")) { added_string = "pyo"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("u")) { added_string = "pyu"; i++; i++; }
                        else { added_string = "*"; }
                    }
                    else if (character_next.equalsIgnoreCase("p")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("q")) { added_string = "*"; }

                else if (character.equalsIgnoreCase("r")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "ra"; i++; }
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "re"; i++; }
                    else if (character_next.equalsIgnoreCase("i")) { added_string = "ri"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "ro"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "ru"; i++; }
                    else if (character_next.equalsIgnoreCase("y")) {
                        if      (character_next2.equalsIgnoreCase("a")) { added_string = "rya"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("e")) { added_string = "rye"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("i")) { added_string = "ryi"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("o")) { added_string = "ryo"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("u")) { added_string = "ryu"; i++; i++; }
                        else { added_string = "*"; }
                    }
                    else if (character_next.equalsIgnoreCase("r")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("s")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "sa"; i++;}
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "se"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "so"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "su"; i++; }
                    else if (character_next.equalsIgnoreCase("h")) {
                        if      (character_next2.equalsIgnoreCase("i")) {  added_string = "shi"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("a")) {  added_string = "sha"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("o")) {  added_string = "sho"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("u")) {  added_string = "shu"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("e")) {  added_string = "she"; i++; i++; }
                        else { added_string = "*"; }
                    }
                    else if (character_next.equalsIgnoreCase("s")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("t")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "ta"; i++;}
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "te"; i++; }
                    else if (character_next.equalsIgnoreCase("i")) { added_string = "ti"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "to"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "tu"; i++; }
                    else if (character_next.equalsIgnoreCase("s")) {
                        if      (character_next2.equalsIgnoreCase("a")) { added_string = "tsa"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("i")) { added_string = "tsi"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("u")) { added_string = "tsu"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("e")) { added_string = "tse"; i++; i++; }
                        else if (character_next2.equalsIgnoreCase("o")) { added_string = "tso"; i++; i++; }
                        else { added_string = "*"; }
                    }
                    else if (character_next.equalsIgnoreCase("t")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("u")) {
                    if (character.equalsIgnoreCase(character_last)) { added_string = "u_double_vowel"; }
                    else { added_string = "u"; }
                }

                else if (character.equalsIgnoreCase("v")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "va"; i++;}
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "ve"; i++; }
                    else if (character_next.equalsIgnoreCase("i")) { added_string = "vi"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "vo"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "vu"; i++; }
                    else if (character_next.equalsIgnoreCase("v")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("w")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "wa"; i++;}
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "we"; i++; }
                    else if (character_next.equalsIgnoreCase("i")) { added_string = "wi"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "wo"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "wu"; i++; }
                    else if (character_next.equalsIgnoreCase("w")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("x")) { added_string = "*"; }

                else if (character.equalsIgnoreCase("y")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "ya"; i++; }
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "ye"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "yo"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "yu"; i++; }
                    else { added_string = "*"; }
                }

                else if (character.equalsIgnoreCase("z")) {
                    if      (character_next.equalsIgnoreCase("a")) { added_string = "za"; i++; }
                    else if (character_next.equalsIgnoreCase("e")) { added_string = "ze"; i++; }
                    else if (character_next.equalsIgnoreCase("o")) { added_string = "zo"; i++; }
                    else if (character_next.equalsIgnoreCase("u")) { added_string = "zu"; i++; }
                    else if (character_next.equalsIgnoreCase("z")) { added_string = "small_tsu"; }
                    else { added_string = "*"; }
                }

            // If not recognized by the algorithm above, lve it as-is
                else {added_string = "original"; }

            List<String> output = new ArrayList<>();
            output.add(Integer.toString(i));
            output.add(added_string);
            return output;
        }
        public static List<String> GetCharBasedOnPhoneme(int i, String added_string, String input_value, int final_index, String character, String character_next, String added_string_last) {

            String added_string_latin = "";
            String added_string_hiragana = "";
            String added_string_katakana = "";

            if (added_string.equals("a"))   { added_string_latin = "a"; added_string_hiragana = "あ"; added_string_katakana = "ア"; }

            else if (added_string.equals("ba"))  { added_string_latin = "ba"; added_string_hiragana = "ば"; added_string_katakana = "バ"; }
            else if (added_string.equals("bi"))  { added_string_latin = "bi"; added_string_hiragana = "び"; added_string_katakana = "ビ"; }
            else if (added_string.equals("bu"))  { added_string_latin = "bu"; added_string_hiragana = "ぶ"; added_string_katakana = "ブ"; }
            else if (added_string.equals("be"))  { added_string_latin = "be"; added_string_hiragana = "べ"; added_string_katakana = "ベ"; }
            else if (added_string.equals("bo"))  { added_string_latin = "bo"; added_string_hiragana = "ぼ"; added_string_katakana = "ボ"; }
            else if (added_string.equals("bya")) { added_string_latin = "bya"; added_string_hiragana = "びゃ"; added_string_katakana = "ビャ"; }
            else if (added_string.equals("byu")) { added_string_latin = "byu"; added_string_hiragana = "びゅ"; added_string_katakana = "ビュ"; }
            else if (added_string.equals("byi")) { added_string_latin = "byi"; added_string_hiragana = "びぃ"; added_string_katakana = "ビィ"; }
            else if (added_string.equals("bye")) { added_string_latin = "bye"; added_string_hiragana = "びぇ"; added_string_katakana = "ビェ"; }
            else if (added_string.equals("byo")) { added_string_latin = "byo"; added_string_hiragana = "びょ"; added_string_katakana = "ビョ"; }

            else if (added_string.equals("cha")) { added_string_latin = "cha"; added_string_hiragana = "ちゃ"; added_string_katakana = "チャ"; }
            else if (added_string.equals("chi")) { added_string_latin = "chi"; added_string_hiragana = "ち"; added_string_katakana = "チ"; }
            else if (added_string.equals("chu")) { added_string_latin = "chu"; added_string_hiragana = "ちゅ"; added_string_katakana = "チュ"; }
            else if (added_string.equals("che")) { added_string_latin = "che"; added_string_hiragana = "ちぇ"; added_string_katakana = "チェ"; }
            else if (added_string.equals("cho")) { added_string_latin = "cho"; added_string_hiragana = "ちょ"; added_string_katakana = "チョ"; }

            else if (added_string.equals("da"))  { added_string_latin = "da"; added_string_hiragana = "だ"; added_string_katakana = "ダ"; }
            else if (added_string.equals("di"))  { added_string_latin = "di"; added_string_hiragana = "ぢ"; added_string_katakana = "ヂ"; }
            else if (added_string.equals("du"))  { added_string_latin = "du"; added_string_hiragana = "づ"; added_string_katakana = "ヅ"; }
            else if (added_string.equals("de"))  { added_string_latin = "de"; added_string_hiragana = "で"; added_string_katakana = "デ"; }
            else if (added_string.equals("do"))  { added_string_latin = "do"; added_string_hiragana = "ど"; added_string_katakana = "ド"; }

            else if (added_string.equals("dja"))  { added_string_latin = "dja"; added_string_hiragana = "＊"; added_string_katakana = "ヂャ"; }
            else if (added_string.equals("dji"))  { added_string_latin = "dji"; added_string_hiragana = "ぢ"; added_string_katakana = "ヂ"; }
            else if (added_string.equals("dju"))  { added_string_latin = "dju"; added_string_hiragana = "＊"; added_string_katakana = "ヂュ"; }
            else if (added_string.equals("dje"))  { added_string_latin = "dje"; added_string_hiragana = "＊"; added_string_katakana = "ヂェ"; }
            else if (added_string.equals("djo"))  { added_string_latin = "djo"; added_string_hiragana = "＊"; added_string_katakana = "ヂョ"; }

            else if (added_string.equals("dza"))  { added_string_latin = "dza"; added_string_hiragana = "＊"; added_string_katakana = "ヅァ"; }
            else if (added_string.equals("dzi"))  { added_string_latin = "dzi"; added_string_hiragana = "＊"; added_string_katakana = "ヅィ"; }
            else if (added_string.equals("dzu"))  { added_string_latin = "dzu"; added_string_hiragana = "＊"; added_string_katakana = "ヅ"; }
            else if (added_string.equals("dze"))  { added_string_latin = "dze"; added_string_hiragana = "＊"; added_string_katakana = "ヅェ"; }
            else if (added_string.equals("dzo"))  { added_string_latin = "dzo"; added_string_hiragana = "＊"; added_string_katakana = "ヅォ"; }

            else if (added_string.equals("e"))   { added_string_latin = "e"; added_string_hiragana = "え"; added_string_katakana = "エ"; }

            else if (added_string.equals("fa"))  { added_string_latin = "fa"; added_string_hiragana = "ふぁ"; added_string_katakana = "ファ"; }
            else if (added_string.equals("fi"))  { added_string_latin = "fi"; added_string_hiragana = "ふぃ"; added_string_katakana = "フィ"; }
            else if (added_string.equals("fu"))  { added_string_latin = "fu"; added_string_hiragana = "ふ"; added_string_katakana = "フ"; }
            else if (added_string.equals("fe"))  { added_string_latin = "fe"; added_string_hiragana = "ふぇ"; added_string_katakana = "フェ"; }
            else if (added_string.equals("fo"))  { added_string_latin = "fo"; added_string_hiragana = "ふぉ"; added_string_katakana = "フォ"; }
            else if (added_string.equals("fya")) { added_string_latin = "fya"; added_string_hiragana = "ふゃ"; added_string_katakana = "フャ"; }
            else if (added_string.equals("fye")) { added_string_latin = "fye"; added_string_hiragana = "ふぇ"; added_string_katakana = "フェ"; }
            else if (added_string.equals("fyi")) { added_string_latin = "fyi"; added_string_hiragana = "ふぃ"; added_string_katakana = "フィ"; }
            else if (added_string.equals("fyu")) { added_string_latin = "fyu"; added_string_hiragana = "ふゅ"; added_string_katakana = "フュ"; }
            else if (added_string.equals("fyo")) { added_string_latin = "fyo"; added_string_hiragana = "ふょ"; added_string_katakana = "フョ"; }

            else if (added_string.equals("ga"))  { added_string_latin = "ga"; added_string_hiragana = "が"; added_string_katakana = "ガ"; }
            else if (added_string.equals("gi"))  { added_string_latin = "gi"; added_string_hiragana = "ぎ"; added_string_katakana = "ギ"; }
            else if (added_string.equals("gu"))  { added_string_latin = "gu"; added_string_hiragana = "ぐ"; added_string_katakana = "グ"; }
            else if (added_string.equals("ge"))  { added_string_latin = "ge"; added_string_hiragana = "げ"; added_string_katakana = "ゲ"; }
            else if (added_string.equals("go"))  { added_string_latin = "go"; added_string_hiragana = "ご"; added_string_katakana = "ゴ"; }
            else if (added_string.equals("gya")) { added_string_latin = "gya"; added_string_hiragana = "ぎゃ"; added_string_katakana = "ギャ"; }
            else if (added_string.equals("gye")) { added_string_latin = "gye"; added_string_hiragana = "ぎぇ"; added_string_katakana = "ギェ"; }
            else if (added_string.equals("gyi")) { added_string_latin = "gyi"; added_string_hiragana = "ぎぃ"; added_string_katakana = "ギィ"; }
            else if (added_string.equals("gyu")) { added_string_latin = "gyu"; added_string_hiragana = "ぎゅ"; added_string_katakana = "ギュ"; }
            else if (added_string.equals("gyo")) { added_string_latin = "gyo"; added_string_hiragana = "ぎょ"; added_string_katakana = "ギョ"; }

            else if (added_string.equals("ha"))  { added_string_latin = "ha"; added_string_hiragana = "は"; added_string_katakana = "ハ"; }
            else if (added_string.equals("hi"))  { added_string_latin = "hi"; added_string_hiragana = "ひ"; added_string_katakana = "ヒ"; }
            else if (added_string.equals("hu"))  { added_string_latin = "hu"; added_string_hiragana = "ふ"; added_string_katakana = "フ"; }
            else if (added_string.equals("he"))  { added_string_latin = "he"; added_string_hiragana = "へ"; added_string_katakana = "ヘ"; }
            else if (added_string.equals("ho"))  { added_string_latin = "ho"; added_string_hiragana = "ほ"; added_string_katakana = "ホ"; }
            else if (added_string.equals("hya")) { added_string_latin = "hya"; added_string_hiragana = "ひゃ"; added_string_katakana = "ヒャ"; }
            else if (added_string.equals("hyi")) { added_string_latin = "hyi"; added_string_hiragana = "ひぃ"; added_string_katakana = "ヒィ"; }
            else if (added_string.equals("hyu")) { added_string_latin = "hyu"; added_string_hiragana = "ひゅ"; added_string_katakana = "ヒュ"; }
            else if (added_string.equals("hye")) { added_string_latin = "hye"; added_string_hiragana = "ひぇ"; added_string_katakana = "ヒェ"; }
            else if (added_string.equals("hyo")) { added_string_latin = "hyo"; added_string_hiragana = "ひょ"; added_string_katakana = "ヒョ";}

            else if (added_string.equals("i"))   { added_string_latin = "i"; added_string_hiragana = "い"; added_string_katakana = "イ"; }

            else if (added_string.equals("ja"))  { added_string_latin = "ja"; added_string_hiragana = "じゃ"; added_string_katakana = "ジャ"; }
            else if (added_string.equals("ji"))  { added_string_latin = "ji"; added_string_hiragana = "じ"; added_string_katakana = "ジ"; }
            else if (added_string.equals("ju"))  { added_string_latin = "ju"; added_string_hiragana = "じゅ"; added_string_katakana = "ジュ"; }
            else if (added_string.equals("je"))  { added_string_latin = "je"; added_string_hiragana = "じぇ"; added_string_katakana = "ジェ"; }
            else if (added_string.equals("jo"))  { added_string_latin = "jo"; added_string_hiragana = "じょ"; added_string_katakana = "ジョ"; }
            else if (added_string.equals("jya")) { added_string_latin = "jya"; added_string_hiragana = "じゃ"; added_string_katakana = "ジャ"; }
            else if (added_string.equals("jyu")) { added_string_latin = "jye"; added_string_hiragana = "じぇ"; added_string_katakana = "ジェ"; }
            else if (added_string.equals("jyu")) { added_string_latin = "jyi"; added_string_hiragana = "じぃ"; added_string_katakana = "ジィ";  }
            else if (added_string.equals("jyu")) { added_string_latin = "jyu"; added_string_hiragana = "じゅ"; added_string_katakana = "ジュ"; }
            else if (added_string.equals("jyo")) { added_string_latin = "jyo"; added_string_hiragana = "じょ"; added_string_katakana = "ジョ"; }

            else if (added_string.equals("ka"))  { added_string_latin = "ka"; added_string_hiragana = "か"; added_string_katakana = "カ"; }
            else if (added_string.equals("ki"))  { added_string_latin = "ki"; added_string_hiragana = "き"; added_string_katakana = "キ"; }
            else if (added_string.equals("ku"))  { added_string_latin = "ku"; added_string_hiragana = "く"; added_string_katakana = "ク"; }
            else if (added_string.equals("ke"))  { added_string_latin = "ke"; added_string_hiragana = "け"; added_string_katakana = "ケ"; }
            else if (added_string.equals("ko"))  { added_string_latin = "ko"; added_string_hiragana = "こ"; added_string_katakana = "コ"; }
            else if (added_string.equals("kya")) { added_string_latin = "kya"; added_string_hiragana = "きゃ"; added_string_katakana = "キャ"; }
            else if (added_string.equals("kye")) { added_string_latin = "kye"; added_string_hiragana = "きぇ"; added_string_katakana = "キェ"; }
            else if (added_string.equals("kyi")) { added_string_latin = "kyi"; added_string_hiragana = "きぃ"; added_string_katakana = "キィ"; }
            else if (added_string.equals("kyu")) { added_string_latin = "kyu"; added_string_hiragana = "きゅ"; added_string_katakana = "キュ"; }
            else if (added_string.equals("kyo")) { added_string_latin = "kyo"; added_string_hiragana = "きょ"; added_string_katakana = "キョ"; }

            else if (added_string.equals("ma"))  { added_string_latin = "ma"; added_string_hiragana = "ま"; added_string_katakana = "マ"; }
            else if (added_string.equals("mi"))  { added_string_latin = "mi"; added_string_hiragana = "み"; added_string_katakana = "ミ"; }
            else if (added_string.equals("mu"))  { added_string_latin = "mu"; added_string_hiragana = "む"; added_string_katakana = "ム"; }
            else if (added_string.equals("me"))  { added_string_latin = "me"; added_string_hiragana = "め"; added_string_katakana = "メ"; }
            else if (added_string.equals("mo"))  { added_string_latin = "mo"; added_string_hiragana = "も"; added_string_katakana = "モ"; }
            else if (added_string.equals("mya")) { added_string_latin = "mya"; added_string_hiragana = "みゃ"; added_string_katakana = "ミャ"; }
            else if (added_string.equals("myu")) { added_string_latin = "myu"; added_string_hiragana = "みゅ"; added_string_katakana = "ミュ"; }
            else if (added_string.equals("myi")) { added_string_latin = "myi"; added_string_hiragana = "みぃ"; added_string_katakana = "ミィ"; }
            else if (added_string.equals("mye")) { added_string_latin = "mye"; added_string_hiragana = "みぇ"; added_string_katakana = "ミェ"; }
            else if (added_string.equals("myo")) { added_string_latin = "myo"; added_string_hiragana = "みょ"; added_string_katakana = "ミョ"; }

            else if (added_string.equals("n"))   { added_string_latin = "n"; added_string_hiragana = "ん"; added_string_katakana = "ン"; }
            else if (added_string.equals("n'"))  { added_string_latin = "n'"; added_string_hiragana = "ん"; added_string_katakana = "ン"; }
            else if (added_string.equals("na"))  { added_string_latin = "na"; added_string_hiragana = "な"; added_string_katakana = "ナ"; }
            else if (added_string.equals("ni"))  { added_string_latin = "ni"; added_string_hiragana = "に"; added_string_katakana = "ニ"; }
            else if (added_string.equals("nu"))  { added_string_latin = "nu"; added_string_hiragana = "ぬ"; added_string_katakana = "ヌ"; }
            else if (added_string.equals("ne"))  { added_string_latin = "ne"; added_string_hiragana = "ね"; added_string_katakana = "ネ"; }
            else if (added_string.equals("no"))  { added_string_latin = "no"; added_string_hiragana = "の"; added_string_katakana = "ノ"; }
            else if (added_string.equals("nya")) { added_string_latin = "nya"; added_string_hiragana = "にゃ"; added_string_katakana = "ニャ"; }
            else if (added_string.equals("nyu")) { added_string_latin = "nyu"; added_string_hiragana = "にゅ"; added_string_katakana = "ニュ"; }
            else if (added_string.equals("nye")) { added_string_latin = "nye"; added_string_hiragana = "にぇ"; added_string_katakana = "ニェ"; }
            else if (added_string.equals("nyi")) { added_string_latin = "nyi"; added_string_hiragana = "にぃ"; added_string_katakana = "ニィ"; }
            else if (added_string.equals("nyo")) { added_string_latin = "nyo"; added_string_hiragana = "にょ"; added_string_katakana = "ニョ"; }

            else if (added_string.equals("o"))   { added_string_latin = "o"; added_string_hiragana = "お"; added_string_katakana = "オ"; }

            else if (added_string.equals("pa"))  { added_string_latin = "pa"; added_string_hiragana = "ぱ"; added_string_katakana = "パ"; }
            else if (added_string.equals("pi"))  { added_string_latin = "pi"; added_string_hiragana = "ぴ"; added_string_katakana = "ビ"; }
            else if (added_string.equals("pu"))  { added_string_latin = "pu"; added_string_hiragana = "ぷ"; added_string_katakana = "ヌ"; }
            else if (added_string.equals("pe"))  { added_string_latin = "pe"; added_string_hiragana = "ぺ"; added_string_katakana = "ペ"; }
            else if (added_string.equals("po"))  { added_string_latin = "po"; added_string_hiragana = "ぽ"; added_string_katakana = "ポ"; }
            else if (added_string.equals("pya")) { added_string_latin = "pya"; added_string_hiragana = "ぴゃ"; added_string_katakana = "ピャ"; }
            else if (added_string.equals("pyu")) { added_string_latin = "pyu"; added_string_hiragana = "ぴゅ"; added_string_katakana = "ピュ"; }
            else if (added_string.equals("pyi")) { added_string_latin = "pyi"; added_string_hiragana = "ぴぃ"; added_string_katakana = "ピィ"; }
            else if (added_string.equals("pye")) { added_string_latin = "pye"; added_string_hiragana = "ぴぇ"; added_string_katakana = "ピェ"; }
            else if (added_string.equals("pyo")) { added_string_latin = "pyo"; added_string_hiragana = "ぴょ"; added_string_katakana = "ピョ"; }

            else if (added_string.equals("ra"))  { added_string_latin = "ra"; added_string_hiragana = "ら"; added_string_katakana = "ラ"; }
            else if (added_string.equals("ri"))  { added_string_latin = "ri"; added_string_hiragana = "り"; added_string_katakana = "リ"; }
            else if (added_string.equals("ru"))  { added_string_latin = "ru"; added_string_hiragana = "る"; added_string_katakana = "ル"; }
            else if (added_string.equals("re"))  { added_string_latin = "re"; added_string_hiragana = "れ"; added_string_katakana = "レ"; }
            else if (added_string.equals("ro"))  { added_string_latin = "ro"; added_string_hiragana = "ろ"; added_string_katakana = "ロ"; }
            else if (added_string.equals("rya")) { added_string_latin = "rya"; added_string_hiragana = "りゃ"; added_string_katakana = "リャ"; }
            else if (added_string.equals("ryu")) { added_string_latin = "ryu"; added_string_hiragana = "りゅ"; added_string_katakana = "リュ"; }
            else if (added_string.equals("ryi")) { added_string_latin = "ryi"; added_string_hiragana = "りぃ"; added_string_katakana = "リィ"; }
            else if (added_string.equals("rye")) { added_string_latin = "rye"; added_string_hiragana = "りぇ"; added_string_katakana = "リェ"; }
            else if (added_string.equals("ryo")) { added_string_latin = "ryo"; added_string_hiragana = "りょ"; added_string_katakana = "リョ"; }

            else if (added_string.equals("sa"))  { added_string_latin = "sa"; added_string_hiragana = "さ"; added_string_katakana = "サ"; }
            else if (added_string.equals("si"))  { added_string_latin = "si"; added_string_hiragana = "＊"; added_string_katakana = "＊"; }
            else if (added_string.equals("su"))  { added_string_latin = "su"; added_string_hiragana = "す"; added_string_katakana = "ス"; }
            else if (added_string.equals("se"))  { added_string_latin = "se"; added_string_hiragana = "せ"; added_string_katakana = "セ"; }
            else if (added_string.equals("so"))  { added_string_latin = "so"; added_string_hiragana = "そ"; added_string_katakana = "ソ"; }

            else if (added_string.equals("sha")) { added_string_latin = "sha"; added_string_hiragana = "しゃ"; added_string_katakana = "シャ"; }
            else if (added_string.equals("shi")) { added_string_latin = "shi"; added_string_hiragana = "し"; added_string_katakana = "シ"; }
            else if (added_string.equals("shu")) { added_string_latin = "shu"; added_string_hiragana = "しゅ"; added_string_katakana = "シュ"; }
            else if (added_string.equals("she")) { added_string_latin = "she"; added_string_hiragana = "しぇ"; added_string_katakana = "シェ"; }
            else if (added_string.equals("sho")) { added_string_latin = "sho"; added_string_hiragana = "しょ"; added_string_katakana = "ショ"; }

            else if (added_string.equals("sya")) { added_string_latin = "sya"; added_string_hiragana = "＊"; added_string_katakana = "＊"; }
            else if (added_string.equals("syu")) { added_string_latin = "syu"; added_string_hiragana = "＊"; added_string_katakana = "＊"; }
            else if (added_string.equals("syo")) { added_string_latin = "syo"; added_string_hiragana = "＊"; added_string_katakana = "＊"; }

            else if (added_string.equals("ta"))  { added_string_latin = "ta"; added_string_hiragana = "た"; added_string_katakana = "タ"; }
            else if (added_string.equals("ti"))  { added_string_latin = "ti"; added_string_hiragana = "＊"; added_string_katakana = "ティ"; }
            else if (added_string.equals("tu"))  { added_string_latin = "tu"; added_string_hiragana = "＊"; added_string_katakana = "テュ"; }
            else if (added_string.equals("te"))  { added_string_latin = "te"; added_string_hiragana = "て"; added_string_katakana = "テ"; }
            else if (added_string.equals("to"))  { added_string_latin = "to"; added_string_hiragana = "と"; added_string_katakana = "ト"; }

            else if (added_string.equals("u"))   { added_string_latin = "u"; added_string_hiragana = "う"; added_string_katakana = "ウ"; }

            else if (added_string.equals("va"))  { added_string_latin = "va"; added_string_hiragana = "ヴぁ"; added_string_katakana = "ヴァ"; }
            else if (added_string.equals("vi"))  { added_string_latin = "vi"; added_string_hiragana = "ヴぃ"; added_string_katakana = "ヴィ"; }
            else if (added_string.equals("vu"))  { added_string_latin = "vu"; added_string_hiragana = "ヴ"; added_string_katakana = "ヴ"; }
            else if (added_string.equals("ve"))  { added_string_latin = "ve"; added_string_hiragana = "ヴぇ"; added_string_katakana = "ヴェ"; }
            else if (added_string.equals("vo"))  { added_string_latin = "vo"; added_string_hiragana = "ヴぉ"; added_string_katakana = "ヴォ"; }

            else if (added_string.equals("wa"))  { added_string_latin = "wa"; added_string_hiragana = "わ"; added_string_katakana = "ワ"; }
            else if (added_string.equals("wi"))  { added_string_latin = "wi"; added_string_hiragana = "うぃ"; added_string_katakana = "ウィ"; }
            else if (added_string.equals("wu"))  { added_string_latin = "wu"; added_string_hiragana = "う"; added_string_katakana = "ウ"; }
            else if (added_string.equals("we"))  { added_string_latin = "we"; added_string_hiragana = "うぇ"; added_string_katakana = "ウェ"; }
            else if (added_string.equals("wo"))  { added_string_latin = "wo"; added_string_hiragana = "を"; added_string_katakana = "ヲ"; }

            else if (added_string.equals("ya"))  { added_string_latin = "ya"; added_string_hiragana = "や"; added_string_katakana = "ヤ"; }
            else if (added_string.equals("yu"))  { added_string_latin = "yu"; added_string_hiragana = "ゆ"; added_string_katakana = "ユ"; }
            else if (added_string.equals("ye"))  { added_string_latin = "ye"; added_string_hiragana = "いぇ"; added_string_katakana = "イェ"; }
            else if (added_string.equals("yo"))  { added_string_latin = "yo"; added_string_hiragana = "よ"; added_string_katakana = "ヨ"; }

            else if (added_string.equals("za"))  { added_string_latin = "za"; added_string_hiragana = "ざ"; added_string_katakana = "ザ"; }
            else if (added_string.equals("zu"))  { added_string_latin = "zu"; added_string_hiragana = "ず"; added_string_katakana = "ズ"; }
            else if (added_string.equals("ze"))  { added_string_latin = "ze"; added_string_hiragana = "ぜ"; added_string_katakana = "ゼ"; }
            else if (added_string.equals("zo"))  { added_string_latin = "zo"; added_string_hiragana = "ぞ"; added_string_katakana = "ゾ"; }

            else if (added_string.equals("Xwi"))  { added_string_latin = "wi"; added_string_hiragana = "ゐ"; added_string_katakana = "ヰ"; }
            else if (added_string.equals("Xwe"))  { added_string_latin = "we"; added_string_hiragana = "ゑ"; added_string_katakana = "ヱ"; }

            else if (added_string.equals("*"))   { added_string_latin = "*"; added_string_hiragana = "＊"; added_string_katakana = "＊"; }

            else if (added_string.equals("a_double_vowel"))   { added_string_latin = "a"; added_string_hiragana = "あ"; added_string_katakana = "ー"; }
            else if (added_string.equals("e_double_vowel"))   { added_string_latin = "e"; added_string_hiragana = "え"; added_string_katakana = "ー"; }
            else if (added_string.equals("i_double_vowel"))   { added_string_latin = "i"; added_string_hiragana = "い"; added_string_katakana = "ー"; }
            else if (added_string.equals("o_double_vowel"))   { added_string_latin = "o"; added_string_hiragana = "お"; added_string_katakana = "ー"; }
            else if (added_string.equals("u_double_vowel"))   { added_string_latin = "u"; added_string_hiragana = "う"; added_string_katakana = "ー"; }
            
            else if (added_string.equals("katakana_repeat_bar"))   {

                if (!added_string_last.equals("")) {
                    if (added_string_last.substring(added_string_last.length()-1,added_string_last.length()).equals("a")) {
                        added_string_latin = "a"; added_string_hiragana = "あ"; added_string_katakana = "ー";
                    }
                    else if (added_string_last.substring(added_string_last.length()-1,added_string_last.length()).equals("i")) {
                        added_string_latin = "i"; added_string_hiragana = "い"; added_string_katakana = "ー";
                    }
                    else if (added_string_last.substring(added_string_last.length()-1,added_string_last.length()).equals("u")) {
                        added_string_latin = "u"; added_string_hiragana = "う"; added_string_katakana = "ー";
                    }
                    else if (added_string_last.substring(added_string_last.length()-1,added_string_last.length()).equals("e")) {
                        added_string_latin = "e"; added_string_hiragana = "え"; added_string_katakana = "ー";
                    }
                    else if (added_string_last.substring(added_string_last.length()-1,added_string_last.length()).equals("o")) {
                        added_string_latin = "o"; added_string_hiragana = "お"; added_string_katakana = "ー";
                    }
                    else if (added_string_last.equals("*"))   { added_string_latin = ""; added_string_hiragana = ""; added_string_katakana = ""; }
                }
            }

            else if (added_string.equals("small_tsu"))  {
                added_string_latin = "";
                added_string_hiragana = "っ";
                added_string_katakana = "ッ";
            }

            char first_char = '*';
            if (!added_string_latin.equals("")) { first_char = added_string_latin.charAt(0);}
            if (added_string_last.equals("small_tsu")) {
                if (added_string_latin.equals("")) { added_string_latin = "*"; } //If the character after small_tsu is invlid (e.g. a kanji), this line prevents the program from crashing
                else {
                    first_char = added_string_latin.charAt(0);
                    if (first_char == 'a' || first_char == 'e' || first_char == 'i' || first_char == 'o' || first_char == 'u' || first_char == 'y') {
                        added_string_latin = "*" + added_string_latin;
                    } else {
                        added_string_latin = first_char + added_string_latin;
                    }
                }
            }

            // Delimiters
            else if (character.equals(",")) {
                added_string_latin = ", ";
                added_string_katakana = ", ";
                added_string_hiragana = ", ";
                if (character_next.equals(" ")) { i++; }
            }

            else if (added_string.equals("original"))   { added_string_latin = character; added_string_hiragana = character; added_string_katakana = character; }

            List<String> output = new ArrayList<>();

            output.add(Integer.toString(i));
            output.add(added_string_latin);
            output.add(added_string_hiragana);
            output.add(added_string_katakana);

            return output;
        }
}