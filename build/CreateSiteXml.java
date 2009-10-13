import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CreateSiteXml {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: " + CreateSiteXml.class.getSimpleName()
                    + " <site.xml template> <features folder>");
            return;
        }

        String siteXmlTemplate = args[0];
        String featuresFolder = args[1];

        Map<String, String> featureVersions = getFeatureVersions(featuresFolder);

        FileReader fr = new FileReader(siteXmlTemplate);
        BufferedReader br = new BufferedReader(fr);
        while (true) {
            String line = br.readLine();
            if (line == null) {
                break;
            }

            for (String feature : featureVersions.keySet()) {
                line = line.replaceAll("@" + feature + "-version@",
                        featureVersions.get(feature));
            }
            System.out.println(line);
        }

    }

    private static Map<String, String> getFeatureVersions(String featuresFolder) {
        Map<String, String> featureVersions = new HashMap<String, String>();

        File featuresDir = new File(featuresFolder);
        for (String entry : featuresDir.list()) {
            if (entry.matches("^.*_\\d\\.\\d\\.\\d\\.(.*)\\.jar")) {
                String feature = entry.replaceFirst("_.*", "");
                String version = entry.replaceFirst(".*_", "").replaceFirst(
                        "\\.jar", "");
                featureVersions.put(feature, version);
            }
        }

        return featureVersions;
    }
}
