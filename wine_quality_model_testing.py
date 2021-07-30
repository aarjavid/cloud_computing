from pyspark.sql import SparkSession
import pyspark.sql.functions as func
from pyspark.ml import Pipeline
from pyspark.ml import PipelineModel
from pyspark.ml.classification import RandomForestClassifier
from pyspark.mllib.evaluation import MulticlassMetrics
from pyspark.ml.evaluation import MulticlassClassificationEvaluator
import sys

filepath = sys.argv[1] #"./ValidationDataset.csv"
modelpath = sys.argv[2]
with SparkSession.builder.appName("Log Regression").getOrCreate() as spark: 
	test_dataset = spark.read.format("csv").options( delimiter=';',header='true', inferSchema='true').csv(filepath) 

	model = PipelineModel.load(modelpath)
	predictions  = model.transform(test_dataset)	
	predictions  = predictions.withColumn("outlabel", predictions['""""quality"""""'].cast('double'))
	predictions  = predictions.withColumn("prediction", func.round(predictions["prediction"]))

	evaluator = MulticlassClassificationEvaluator(
	    labelCol="outlabel", predictionCol="prediction", metricName="accuracy")
	accuracy = round(evaluator.evaluate(predictions),2)	
	evaluator = MulticlassClassificationEvaluator(
	    labelCol="outlabel", predictionCol="prediction", metricName="f1")
	f1  = round(evaluator.evaluate(predictions),2)

	metric_params  = (predictions.select(['prediction', 'outlabel'])).rdd	
	perf_metr = MulticlassMetrics(metric_params)
	prec = {}
	f1score = {}
	recall = {}
	
	classes = [x.outlabel for x in predictions.select('outlabel').distinct().collect()]

	for iter in range(0, 10):
		if iter in classes:
			prec[iter] = round(perf_metr.precision(iter), 2)
			recall[iter] = round(perf_metr.recall(iter), 2)
			f1score[iter] = round(perf_metr.fMeasure(float(iter),1.0),2)

	print("\nDetailed Metrics for every class label:-\n")
	print("Class:    F1 Score:    Precision:    Recall:")
	for iter in range(0, 10):
		if iter in classes:
			print(f'{iter}{f1score[iter]:13}{prec[iter]:15}{recall[iter]:10}')

	print("\n********\n\nOverall Stats\n\n*******\n")
	print("Accuracy=  ",accuracy)
	print("f1score=  ",f1)	
	print("\n********\nProgram Over\n\n*******\n")
