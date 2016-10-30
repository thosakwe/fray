import 'dart:async';
import 'package:fray/fray.dart';

main(_) {
  runZoned(() => new FrayAnalysisServer().listen(null, 3000), onError: main);
}