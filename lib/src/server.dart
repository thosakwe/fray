import 'package:nero/nero.dart';

class FrayAnalysisServer extends Nero {
  FrayAnalysisServer() :super() {
    post('/subscribe', (req) async {
      Response subscribeToFiles(List<String> files) {
        final List added = [];

        if (!req.session.containsKey('added')) {
          req.session['added'] = <String>[];
        }

        for (final file in files) {
          if (!req.session['added'].contains(file)) {
            req.session['added'].add(file);
            added.add(file);
          }
        }

        return new Response.json({'added': added});
      }

      if (req.body.containsKey('file')) {
        return subscribeToFiles([req.body['file']]);
      } else if (req.body.containsKey('files')) {
        return subscribeToFiles(req.body['files']);
      } else
        return new Response.text('400 Bad Request')
          ..statusCode = 400;
    });
  }
}