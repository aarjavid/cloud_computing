import sys
from pyspark.ml.feature import VectorAssembler  
from pyspark.sql import SparkSession
from pyspark.ml import Pipeline
from pyspark.ml.classification import RandomForestClassifier

filepath = sys.argv[1]  # "./TrainingDataSet.csv"
print("\n\nfilepath=",filepath,"---")
with SparkSession.builder.appName("Log Regression").master("local[4]").getOrCreate() as spark: 
	train_dataset = spark.read.format("csv").options( delimiter=';',header='true', inferSchema='true').csv(filepath) 
	featureColumns = [c for c in train_dataset.columns if c != '""""quality"""""']
	assembler = VectorAssembler(inputCols=featureColumns,outputCol="features")
	df  = assembler.transform(train_dataset)
	model = RandomForestClassifier(featuresCol='features', labelCol='""""quality"""""', numTrees=200)
	pipeline = Pipeline(stages=[assembler, model])
	pipeline_fitted_model  = pipeline.fit(train_dataset)
	pipeline_fitted_model.write().overwrite().save("s3://winequalitycheck/SavedModels/wine_LR")
